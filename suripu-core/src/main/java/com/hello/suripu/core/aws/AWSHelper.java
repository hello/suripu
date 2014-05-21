package com.hello.suripu.core.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;

public class AWSHelper {

    public static AWSCredentialsProvider getCredentials() {
        AWSCredentialsProvider awsCredentialsProvider = new InstanceProfileCredentialsProvider();
        try {
            awsCredentialsProvider.getCredentials();
        } catch (AmazonClientException exception) {
            awsCredentialsProvider = new EnvironmentVariableCredentialsProvider();
            awsCredentialsProvider.getCredentials();
        }

        return awsCredentialsProvider;
    }
}
