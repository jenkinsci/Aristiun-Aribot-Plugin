package io.jenkins.plugins.aribot;

import javax.annotation.Nonnull;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureCredentials;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


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

  @DataBoundSetter
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
