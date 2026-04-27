const { fromIni } = require('@aws-sdk/credential-provider-ini');

class AwsAuthClient {
  /**
   * Return valid AWS credentials using sso profile to perform authentication
   *
   * @param profile   profile to use during authentication
   * @param isLocal   if local return mocked credentials
   *
   * @return AWS temporary credentials
   * */
  async getCredentials(profile, isLocal) {
    if (isLocal) {
      return {
        accessKeyId: 'local',
        secretAccessKey: 'local',
        sessionToken: 'local',
      };
    }

    return fromIni({
      profile: profile,
      mfaCodeProvider: async () => {
        const mfaToken = process.env.MFA_TOKEN;
        if (!mfaToken) {
          throw new Error("MFA token not found in environment variables (MFA_TOKEN)");
        }
        return mfaToken;
      }
    })();
  }
}

exports.AwsAuthClient = AwsAuthClient;
