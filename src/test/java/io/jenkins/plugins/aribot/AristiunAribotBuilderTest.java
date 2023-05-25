package io.jenkins.plugins.aribot;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.plugins.credentials.*;
import com.microsoft.azure.util.AzureCredentials;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


public class AristiunAribotBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final CredentialsScope scope = CredentialsScope.valueOf("USER");
    AzureCredentials credentialsAzure = new ExtendedAzureCredentials(
            scope,
            "aribot-test-azure",
            "Test credentials",
            "d56f890ea2a7-414d-9aaa-2b5d87706c6c",
            "62f84ec1-5bf9-4386-b07b-966f581867e1",
            Secret.fromString("48cd5d24-8228-4ad4-9596-74b051ab7785"),
            "staging"
    );

    final AWSCredentialsImpl credentialsAWS = new AWSCredentialsImpl(
            scope,
            "aribot-test-aws",
            "accessKey",
            "secretKey",
            "description"
    );

    final String credentialsAzureId = credentialsAzure.getId();


    final String credentialsAWSId = credentialsAWS.getId();

    @Test
    public void testConfigRoundtrip() throws Exception {
        // Add test credentials to the Jenkins
        SystemCredentialsProvider provider = SystemCredentialsProvider.getInstance();
        provider.getCredentials().add(credentialsAzure);

        FreeStyleProject project = jenkins.createFreeStyleProject();

        AristiunAribotBuilder builder = new io.jenkins.plugins.aribot.AristiunAribotBuilder(credentialsAzureId);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AristiunAribotBuilder aribot = new io.jenkins.plugins.aribot.AristiunAribotBuilder(credentialsAzureId);
        jenkins.assertEqualDataBoundBeans(aribot, project.getBuildersList().get(0));
    }

    @Test
    public void testBuildAzure() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        io.jenkins.plugins.aribot.AristiunAribotBuilder builder = new io.jenkins.plugins.aribot.AristiunAribotBuilder(credentialsAzureId);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Starting Aribot", build);
    }

    @Test
    public void testBuildAWS() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        io.jenkins.plugins.aribot.AristiunAribotBuilder builder = new io.jenkins.plugins.aribot.AristiunAribotBuilder(credentialsAWSId);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Starting Aribot", build);
    }
}