package tv.hd3g.fflauncher;

import tv.hd3g.commons.codepolicyvalidation.CheckPolicy;

class CodePolicyValidation extends CheckPolicy {

	/**
	 * Disable noSysOutSysErr checks
	 */
	@Override
	public void noSysOutSysErr() {
	}

}
