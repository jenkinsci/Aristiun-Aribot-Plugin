/*
 * The MIT License
 *
 * Copyright (c) [2023] [Aristiun B.V.]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.aribot;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.ConnectException;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import jenkins.model.Jenkins;


public class AristiunAribotBuilder extends Builder implements SimpleBuildStep {

    private String name;
    private String credentials;
    private final static String aribotPipelineRegistrationUrl = "https://aribot.aristiun.com/pipeline-api/register/";
    private final static String aribotUrl = "https://aribot.aristiun.com/";

    @DataBoundConstructor
    public AristiunAribotBuilder(String credentials) {
        this.credentials = credentials;
        this.name = "Aristiun Aribot";
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    @DataBoundSetter
    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getCredentials() { return credentials; }


    private void processResponse(CloseableHttpResponse response, TaskListener listener) throws IOException {
        String responseString = EntityUtils.toString(response.getEntity());
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200 && statusCode != 201) {
            listener.getLogger().printf("Request failed. Response was %s%n", responseString);
        } else {
            JSONObject jsonObj = new JSONObject(responseString);
            String code = jsonObj.getString("code");
            switch (code) {
                case "reg_link":
                    listener.getLogger().printf(
                            "Your registration link is %spipeline-onboarding/?token=%s%n",
                            this.aribotUrl,
                            jsonObj.getString("registration_token")
                    );
                    break;
                case "ok":
                    listener.getLogger().printf("%s%n", jsonObj.getString("message"));
                    break;
                case "account_created":
                    listener.getLogger().printf(
                            "New account was created. Please proceed to Aristiun Aribot to use it. %s%n",
                            this.aribotUrl
                    );
                    break;
                default:
                    break;
            }
        }
    }

    private void doRegisterAzurePipeline(ExtendedAzureCredentials credentials, TaskListener listener) {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(String.format("%s%s", this.aribotPipelineRegistrationUrl, "?pipeline=jenkins_azure"));
        httpPost.setHeader("Content-type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("subscription_id", credentials.getSubscriptionId());
        payload.put("resource_group_name", credentials.getResourceGroupName());
        payload.put("tenant_id", credentials.getTenant());
        payload.put("account_name", this.getName());
        payload.put("client_id", credentials.getClientId());
        payload.put("client_secret", credentials.getPlainClientSecret());
        httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        try {
            CloseableHttpResponse response = httpclient.execute(httpPost);
            this.processResponse(response, listener);
        } catch (ConnectException e) {
            System.out.println(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doRegisterAWSPipeline(AWSCredentialsImpl credentials, TaskListener listener) {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(String.format("%s%s", this.aribotPipelineRegistrationUrl, "?pipeline=jenkins_aws"));
        httpPost.setHeader("Content-type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("access_key", credentials.getAccessKey());
        payload.put("secret_access_key", credentials.getSecretKey());
        httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        try {
            CloseableHttpResponse response = httpclient.execute(httpPost);
            this.processResponse(response, listener);
        } catch (ConnectException e) {
            System.out.println(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerPipeline(Run run, TaskListener listener) {
        ExtendedAzureCredentials azureCredentials = CredentialsProvider.findCredentialById(
                this.credentials,
                ExtendedAzureCredentials.class,
                run
        );
        if (azureCredentials != null) {
            this.doRegisterAzurePipeline(azureCredentials, listener);
        };

        AWSCredentialsImpl awsCredentials = CredentialsProvider.findCredentialById(
                this.credentials,
                AWSCredentialsImpl.class,
                run
        );
        if (awsCredentials != null) {
            this.doRegisterAWSPipeline(awsCredentials, listener);
        };
    };

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Starting Aribot");
        this.registerPipeline(run, listener);
    }

    @Symbol("aribot")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.AristiunAribotBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.AristiunAribotBuilder_DescriptorImpl_warnings_tooShort());

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public ListBoxModel doFillCredentialsItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel model = new StandardListBoxModel();

            if (item == null) {
                Jenkins jenkins = Jenkins.get();
                if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
                    return model.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return model.includeCurrentValue(credentialsId);
                }
            }
            return model.includeAs(ACL.SYSTEM, item, ExtendedAzureCredentials.class)
                    .includeAs(ACL.SYSTEM, item, AWSCredentialsImpl.class)
                    .includeCurrentValue(credentialsId);
        }

        public String getIconFileName() {
            return "/plugin/aribot/img/logo.png";
        }

        @Override
        public String getDisplayName() {
            return Messages.AristiunAribotBuilder_DescriptorImpl_DisplayName();
        }
    }
}
