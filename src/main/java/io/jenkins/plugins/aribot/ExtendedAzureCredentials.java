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

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;


/**
 * Extended implementation of {@link AzureCredentials} with additional resource group name field.
 */
public class ExtendedAzureCredentials extends AzureCredentials {

  private String resourceGroupName;


  @DataBoundConstructor
  public ExtendedAzureCredentials(CredentialsScope scope, String id, String description, String subscriptionId, String clientId, String clientSecret, String resourceGroupName) {
    super(scope, id, description, subscriptionId, clientId, clientSecret);
    this.resourceGroupName = resourceGroupName;
  }

  public void setResourceGroupName(String resourceGroupName) {
    this.resourceGroupName = resourceGroupName;
  }

  public String getResourceGroupName() { return resourceGroupName; }

  @Extension
  public static class ExtendedAzureCredentialsDescriptor extends DescriptorImpl {

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Azure Credentials (Aribot)";
    }
  }
}
