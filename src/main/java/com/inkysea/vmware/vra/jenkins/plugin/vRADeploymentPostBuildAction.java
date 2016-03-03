/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.inkysea.vmware.vra.jenkins.plugin;

import com.inkysea.vmware.vra.jenkins.plugin.model.Deployment;
import com.inkysea.vmware.vra.jenkins.plugin.model.PluginParam;
import com.inkysea.vmware.vra.jenkins.plugin.model.RequestParam;
import com.inkysea.vmware.vra.jenkins.plugin.util.EnvVariableResolver;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class vRADeploymentPostBuildAction extends Notifier {

	private static final Logger LOGGER = Logger.getLogger(vRADeploymentPostBuildAction.class.getName());

	protected List<PluginParam> params;
	protected List<Deployment> deployments = new ArrayList<Deployment>();
	private List<RequestParam> requestParams;

	@DataBoundConstructor
	public vRADeploymentPostBuildAction(List<PluginParam> params) {
		this.params = params;
	}

	public List<PluginParam> getParams() {
		return params;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		LOGGER.info("prebuild");
		return super.prebuild(build, listener);
	}


	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectAction");
		return super.getProjectAction(project);
	}

	@Override
	public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectActions");
		return super.getProjectActions(project);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {

		EnvVars env = build.getEnvironment(listener);
		env.overrideAll(build.getBuildVariables());
		EnvVariableResolver helper = new EnvVariableResolver(build, listener);


		boolean success = true;

		int counter = 1;
		for (PluginParam param : params) {

			// Resolve any environment variables in the parameters
			PluginParam fparam = new PluginParam(helper.replaceBuildParamWithValue(param.getServerUrl()),
					helper.replaceBuildParamWithValue(param.getUserName()),
					helper.replaceBuildParamWithValue(param.getPassword()),
					helper.replaceBuildParamWithValue(param.getTenant()),
					helper.replaceBuildParamWithValue(param.getBluePrintName()),
					param.isWaitExec(), param.getRequestParams());

			final Deployment deployment = newDeployment(listener.getLogger(), fparam);


			if (deployment.Create()) {
				this.deployments.add(deployment);

				//change counter to string and append pb for build environment
				String strCounter = Integer.toString(counter)+"pb";
				env.putAll(deployment.getDeploymentComponents(strCounter));
				counter++;
			} else {
				build.setResult(Result.FAILURE);
				success = false;
				break;
			}

		}
		return success;

	}


	protected Deployment newDeployment(PrintStream logger, PluginParam params) throws IOException {

		Boolean isURL = false;
		String recipe = null;

		return new Deployment(logger, params);

	}

	@Override
	public BuildStepDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final vRADeploymentPostBuildAction.DescriptorImpl DESCRIPTOR = new vRADeploymentPostBuildAction.DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public String getDisplayName() {
                    
			return "vRealize Automation Deployment";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
                
	}
}