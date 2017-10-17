package software.wings.service.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClientBuilder;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupResult;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentResult;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecr.model.Repository;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DeleteServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionRequest;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionResult;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/15/16.
 */
@Singleton
public class AwsHelperService {
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void validateAwsAccountCredential(String accessKey, char[] secretKey) {
    try {
      new AmazonEC2Client(new BasicAWSCredentials(accessKey, new String(secretKey))).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER, "message", "Invalid AWS credentials.");
      }
    }
  }

  /**
   * Gets aws cloud watch client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the aws cloud watch client
   */
  private AmazonCloudWatchClient getAwsCloudWatchClient(String region, String accessKey, char[] secretKey) {
    return (AmazonCloudWatchClient) AmazonCloudWatchClientBuilder.standard()
        .withRegion(region)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, String.valueOf(secretKey))))
        .build();
  }

  /**
   * Gets amazon ecs client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ecs client
   */
  private AmazonECSClient getAmazonEcsClient(String region, String accessKey, char[] secretKey) {
    return (AmazonECSClient) AmazonECSClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  private AWSLambdaClient getAmazonLambdaClient(String region, String accessKey, char[] secretKey) {
    return (AWSLambdaClient) AWSLambdaClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  private AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(awsConfig.getAccessKey(), new String(awsConfig.getSecretKey()))))
        .build();
  }

  public AmazonECRClient getAmazonEcrClient(EcrConfig ecrConfig) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(ecrConfig.getRegion())
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
        .build();
  }

  /**
   * Gets amazon ecr client.
   *
   * @return the auth token
   */
  public String getAmazonEcrAuthToken(EcrConfig ecrConfig) {
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(ecrConfig.getRegion())
                                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                                        ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
                                    .build();

    String url = ecrConfig.getEcrUrl();
    // Example: https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
    String awsAccount = url.substring(8, url.indexOf("."));
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }

  public String getAmazonEcrAuthToken(String awsAccount, String region, String accessKey, char[] secretKey) {
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(region)
                                    .withCredentials(new AWSStaticCredentialsProvider(
                                        new BasicAWSCredentials(accessKey, new String(secretKey))))
                                    .build();
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }

  /**
   * Gets amazon s3 client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon s3 client
   */
  private AmazonS3Client getAmazonS3Client(String accessKey, char[] secretKey, String region) {
    return (AmazonS3Client) AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, String.valueOf(secretKey))))
        .build();
  }

  /**
   * Gets amazon ec 2 client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ec 2 client
   */
  private AmazonEC2Client getAmazonEc2Client(String region, String accessKey, char[] secretKey) {
    return (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon identity management client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon identity management client
   */
  private AmazonIdentityManagementClient getAmazonIdentityManagementClient(String accessKey, char[] secretKey) {
    return new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  /**
   * Gets amazon code deploy client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon code deploy client
   */
  private AmazonCodeDeployClient getAmazonCodeDeployClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonCodeDeployClient) AmazonCodeDeployClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon cloud formation client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon cloud formation client
   */
  private AmazonCloudFormationClient getAmazonCloudFormationClient(String accessKey, char[] secretKey) {
    return new AmazonCloudFormationClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  /**
   * Gets amazon auto scaling client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon auto scaling client
   */
  private AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonAutoScalingClient) AmazonAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon elastic load balancing client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon elastic load balancing client
   */
  private AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(
      Regions region, String accessKey, char[] secretKey) {
    return (AmazonElasticLoadBalancingClient) com.amazonaws.services.elasticloadbalancingv2
        .AmazonElasticLoadBalancingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets classic elb client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the classic elb client
   */
  private com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
      Regions region, String accessKey, char[] secretKey) {
    return (com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient)
        AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(region)
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
            .build();
  }

  /**
   * Gets hostname from dns name.
   *
   * @param dnsName the dns name
   * @return the hostname from dns name
   */
  public String getHostnameFromPrivateDnsName(String dnsName) {
    return isNotEmpty(dnsName) ? dnsName.split("\\.")[0] : "";
  }

  /**
   * Gets instance id.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @param hostName  the host name
   * @return the instance id
   */
  public String getInstanceId(Regions region, String accessKey, char[] secretKey, String hostName) {
    AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region.getName(), accessKey, secretKey);

    String instanceId;
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(
        new DescribeInstancesRequest().withFilters(new Filter("private-dns-name").withValues(hostName + "*")));
    instanceId = describeInstancesResult.getReservations()
                     .stream()
                     .flatMap(reservation -> reservation.getInstances().stream())
                     .map(Instance::getInstanceId)
                     .findFirst()
                     .orElse(null);

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("private-ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("dns-name").withValues(hostName + "*")));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }
    return instanceId;
  }

  /**
   * Gets id from arn.
   *
   * @param arn the arn
   * @return the id from arn
   */
  public String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  /**
   * Can connect to host boolean.
   *
   * @param hostName the host name
   * @param port     the port
   * @param timeout  the timeout
   * @return the boolean
   */
  public boolean canConnectToHost(String hostName, int port, int timeout) {
    Socket client = new Socket();
    try {
      client.connect(new InetSocketAddress(hostName, port), timeout);
      client.close();
      return true;
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      e.printStackTrace();
      return false;
    } finally {
      IOUtils.closeQuietly(client);
    }
  }

  public List<Bucket> listS3Buckets(AwsConfig awsConfig) {
    try {
      return getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1").listBuckets();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Collections.emptyList();
  }

  public S3Object getObjectFromS3(AwsConfig awsConfig, String bucketName, String key) {
    try {
      return getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, bucketName))
          .getObject(bucketName, key);

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public ObjectMetadata getObjectMetadataFromS3(AwsConfig awsConfig, String bucketName, String key) {
    try {
      return getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, bucketName))
          .getObjectMetadata(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public ListObjectsV2Result listObjectsInS3(AwsConfig awsConfig, ListObjectsV2Request listObjectsV2Request) {
    try {
      AmazonS3Client amazonS3Client = getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          getBucketRegion(awsConfig, listObjectsV2Request.getBucketName()));
      return amazonS3Client.listObjectsV2(listObjectsV2Request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListObjectsV2Result();
  }

  public AwsConfig validateAndGetAwsConfig(SettingAttribute connectorConfig) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "connectorConfig is not of type AwsConfig");
    }
    return (AwsConfig) connectorConfig.getValue();
  }

  private void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND, "message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND, "message", amazonServiceException.getMessage());
    } else {
      logger.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getMessage());
    }
  }

  public ListDeploymentGroupsResult listDeploymentGroupsResult(
      AwsConfig awsConfig, String region, ListDeploymentGroupsRequest listDeploymentGroupsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentGroups(listDeploymentGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentGroupsResult();
  }

  public ListApplicationsResult listApplicationsResult(
      AwsConfig awsConfig, String region, ListApplicationsRequest listApplicationsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listApplications(listApplicationsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListApplicationsResult();
  }

  public ListDeploymentConfigsResult listDeploymentConfigsResult(
      AwsConfig awsConfig, String region, ListDeploymentConfigsRequest listDeploymentConfigsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentConfigs(listDeploymentConfigsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentConfigsResult();
  }

  public GetDeploymentResult getCodeDeployDeployment(
      AwsConfig awsConfig, String region, GetDeploymentRequest getDeploymentRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeployment(getDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentResult();
  }

  public GetDeploymentGroupResult getCodeDeployDeploymentGroup(
      AwsConfig awsConfig, String region, GetDeploymentGroupRequest getDeploymentGroupRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeploymentGroup(getDeploymentGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentGroupResult();
  }

  public CreateDeploymentResult createCodeDeployDeployment(
      AwsConfig awsConfig, String region, CreateDeploymentRequest createDeploymentRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createDeployment(createDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateDeploymentResult();
  }

  public ListDeploymentInstancesResult listDeploymentInstances(
      AwsConfig awsConfig, String region, ListDeploymentInstancesRequest listDeploymentInstancesRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentInstances(listDeploymentInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentInstancesResult();
  }

  public DescribeInstancesResult describeEc2Instances(
      AwsConfig awsConfig, String region, DescribeInstancesRequest describeInstancesRequest) {
    try {
      return getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeInstances(describeInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstancesResult();
  }

  public TerminateInstancesResult terminateEc2Instances(AwsConfig awsConfig, String region, List<String> instancesIds) {
    try {
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.terminateInstances(new TerminateInstancesRequest(instancesIds));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new TerminateInstancesResult();
  }

  public DescribeImagesResult decribeEc2Images(
      AwsConfig awsConfig, String region, DescribeImagesRequest describeImagesRequest) {
    try {
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.describeImages(describeImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeImagesResult();
  }

  public List<String> listRegions(AwsConfig awsConfig) {
    try {
      AmazonEC2Client amazonEC2Client =
          getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listVPCs(AwsConfig awsConfig, String region) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client
          .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter("state").withValues("available")))
          .getVpcs()
          .stream()
          .map(Vpc::getVpcId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listSecurityGroupIds(AwsConfig awsConfig, String region, List<String> vpcIds) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      return amazonEC2Client.describeSecurityGroups(new DescribeSecurityGroupsRequest().withFilters(filters))
          .getSecurityGroups()
          .stream()
          .map(SecurityGroup::getGroupId)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listSubnetIds(AwsConfig awsConfig, String region, List<String> vpcIds) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      filters.add(new Filter("state").withValues("available"));
      return amazonEC2Client.describeSubnets(new DescribeSubnetsRequest().withFilters(filters))
          .getSubnets()
          .stream()
          .map(Subnet::getSubnetId)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public Set<String> listTags(AwsConfig awsConfig, String region) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client
          .describeTags(new DescribeTagsRequest()
                            .withFilters(new Filter("resource-type").withValues("instance"))
                            .withMaxResults(1000))
          .getTags()
          .stream()
          .map(TagDescription::getKey)
          .collect(Collectors.toSet());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptySet();
  }

  public List<String> listAutoScalingGroups(AwsConfig awsConfig, String region) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient
          .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withMaxRecords(100))
          .getAutoScalingGroups()
          .stream()
          .map(AutoScalingGroup::getAutoScalingGroupName)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public CreateClusterResult createCluster(
      String region, AwsConfig awsConfig, CreateClusterRequest createClusterRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createCluster(createClusterRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateClusterResult();
  }

  public DescribeClustersResult describeClusters(
      String region, AwsConfig awsConfig, DescribeClustersRequest describeClustersRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeClusters(describeClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeClustersResult();
  }

  public ListClustersResult listClusters(String region, AwsConfig awsConfig, ListClustersRequest listClustersRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listClusters(listClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListClustersResult();
  }

  public RegisterTaskDefinitionResult registerTaskDefinition(
      String region, AwsConfig awsConfig, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .registerTaskDefinition(registerTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new RegisterTaskDefinitionResult();
  }

  public ListServicesResult listServices(String region, AwsConfig awsConfig, ListServicesRequest listServicesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listServices(listServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListServicesResult();
  }

  public DescribeServicesResult describeServices(
      String region, AwsConfig awsConfig, DescribeServicesRequest describeServicesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeServices(describeServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeServicesResult();
  }

  public CreateServiceResult createService(
      String region, AwsConfig awsConfig, CreateServiceRequest createServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createService(createServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateServiceResult();
  }

  public UpdateServiceResult updateService(
      String region, AwsConfig awsConfig, UpdateServiceRequest updateServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .updateService(updateServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateServiceResult();
  }

  public DeleteServiceResult deleteService(
      String region, AwsConfig awsConfig, DeleteServiceRequest deleteServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .deleteService(deleteServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DeleteServiceResult();
  }

  public ListTasksResult listTasks(String region, AwsConfig awsConfig, ListTasksRequest listTasksRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey()).listTasks(listTasksRequest);
    } catch (ClusterNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND, "message", ex.getMessage());
    } catch (ServiceNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND, "message", ex.getMessage());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListTasksResult();
  }

  public ListTaskDefinitionsResult listTaskDefinitions(
      String region, AwsConfig awsConfig, ListTaskDefinitionsRequest listTaskDefinitionsRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listTaskDefinitions(listTaskDefinitionsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListTaskDefinitionsResult();
  }

  public DescribeTasksResult describeTasks(
      String region, AwsConfig awsConfig, DescribeTasksRequest describeTasksRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTasks(describeTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTasksResult();
  }

  public DescribeTaskDefinitionResult describeTaskDefinitions(
      String region, AwsConfig awsConfig, DescribeTaskDefinitionRequest describeTaskDefinitionRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTaskDefinition(describeTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTaskDefinitionResult();
  }

  public DescribeContainerInstancesResult describeContainerInstances(
      String region, AwsConfig awsConfig, DescribeContainerInstancesRequest describeContainerInstancesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeContainerInstances(describeContainerInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeContainerInstancesResult();
  }

  public ListImagesResult listEcrImages(AwsConfig awsConfig, String region, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(awsConfig, region).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public ListImagesResult listEcrImages(EcrConfig ecrConfig, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(
      EcrConfig ecrConfig, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public DescribeRepositoriesResult listRepositories(
      AwsConfig awsConfig, DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public Repository getRepository(AwsConfig awsConfig, String region, String repositoryName) {
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    describeRepositoriesRequest.setRepositoryNames(Lists.newArrayList(repositoryName));
    DescribeRepositoriesResult describeRepositoriesResult =
        listRepositories(awsConfig, describeRepositoriesRequest, region);
    List<Repository> repositories = describeRepositoriesResult.getRepositories();
    if (repositories != null && repositories.size() > 0) {
      return repositories.get(0);
    }
    return null;
  }

  public List<LoadBalancerDescription> getLoadBalancerDescriptions(String region, AwsConfig awsConfig) {
    try {
      return getClassicElbClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeLoadBalancers(
              new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancerDescriptions();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<TargetGroup> listTargetGroupsForElb(String region, AwsConfig awsConfig, String loadBalancerName) {
    try {
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

      String loadBalancerArn =
          amazonElasticLoadBalancingClient
              .describeLoadBalancers(new DescribeLoadBalancersRequest().withNames(loadBalancerName))
              .getLoadBalancers()
              .get(0)
              .getLoadBalancerArn();

      return amazonElasticLoadBalancingClient
          .describeTargetGroups(
              new DescribeTargetGroupsRequest().withPageSize(400).withLoadBalancerArn(loadBalancerArn))
          .getTargetGroups();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public void setAutoScalingGroupCapacity(AwsConfig awsConfig, AwsInfrastructureMapping infrastructureMapping) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(
          Regions.fromName(infrastructureMapping.getRegion()), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      amazonAutoScalingClient.setDesiredCapacity(
          new SetDesiredCapacityRequest()
              .withAutoScalingGroupName(infrastructureMapping.getAutoScalingGroupName())
              .withDesiredCapacity(infrastructureMapping.getDesiredCapacity()));
      waitForAllInstancesToBeReady(awsConfig, infrastructureMapping);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  private AutoScalingGroup getAutoScalingGroup(AwsConfig awsConfig, AwsInfrastructureMapping infrastructureMapping) {
    AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(
        Regions.fromName(infrastructureMapping.getRegion()), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return amazonAutoScalingClient
        .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
            infrastructureMapping.getAutoScalingGroupName()))
        .getAutoScalingGroups()
        .iterator()
        .next();
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, AwsInfrastructureMapping infrastructureMapping) {
    Misc.quietSleep(1, TimeUnit.SECONDS);
    int retryCount = RETRY_COUNTER;
    List<String> instanceIds = listInstanceIdsFromAutoScalingGroup(awsConfig, infrastructureMapping);
    while (instanceIds.size() != infrastructureMapping.getDesiredCapacity()
        || !allInstanceInReadyState(awsConfig, infrastructureMapping.getRegion(), instanceIds)) {
      if (retryCount-- <= 0) {
        throw new WingsException(INIT_TIMEOUT, "message", "Not all instances in running state");
      }
      logger.info("Waiting for all instances to be in running state");
      Misc.sleepWithRuntimeException(SLEEP_INTERVAL);
      instanceIds = listInstanceIdsFromAutoScalingGroup(awsConfig, infrastructureMapping);
    }
  }

  private boolean allInstanceInReadyState(AwsConfig awsConfig, String region, List<String> instanceIds) {
    DescribeInstancesResult describeInstancesResult =
        describeEc2Instances(awsConfig, region, new DescribeInstancesRequest().withInstanceIds(instanceIds));
    return describeInstancesResult.getReservations()
        .stream()
        .flatMap(reservation -> reservation.getInstances().stream())
        .allMatch(instance -> instance.getState().getName().equals("running"));
  }

  DescribeInstancesResult describeAutoScalingGroupInstances(
      AwsConfig awsConfig, AwsInfrastructureMapping infrastructureMapping) {
    try {
      AmazonEC2Client amazonEc2Client =
          getAmazonEc2Client(infrastructureMapping.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<String> instanceIds = listInstanceIdsFromAutoScalingGroup(awsConfig, infrastructureMapping);
      return amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstancesResult();
  }

  public List<String> listInstanceIdsFromAutoScalingGroup(
      AwsConfig awsConfig, AwsInfrastructureMapping infrastructureMapping) {
    return getAutoScalingGroup(awsConfig, infrastructureMapping)
        .getInstances()
        .stream()
        .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
        .collect(toList());
  }

  List<String> listIAMInstanceRoles(AwsConfig awsConfig) {
    try {
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonIdentityManagementClient.listInstanceProfiles()
          .getInstanceProfiles()
          .stream()
          .map(InstanceProfile::getInstanceProfileName)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  Map<String, String> listIAMRoles(AwsConfig awsConfig) {
    try {
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

      return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
          .getRoles()
          .stream()
          .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyMap();
  }

  List<String> listApplicationLoadBalancers(AwsConfig awsConfig, String region) {
    try {
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonElasticLoadBalancingClient
          .describeLoadBalancers(new DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancers()
          .stream()
          .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "application"))
          .map(LoadBalancer::getLoadBalancerName)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  List<String> listClassicLoadBalancers(AwsConfig awsConfig, String region) {
    try {
      List<LoadBalancerDescription> describeLoadBalancers = getLoadBalancerDescriptions(region, awsConfig);
      return describeLoadBalancers.stream()
          .map(LoadBalancerDescription::getLoadBalancerName)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public CreateAutoScalingGroupResult createAutoScalingGroup(
      AwsConfig awsConfig, String region, CreateAutoScalingGroupRequest createAutoScalingGroupRequest) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAutoScalingGroupResult();
  }

  public DescribeAutoScalingGroupsResult describeAutoScalingGroups(
      AwsConfig awsConfig, String region, DescribeAutoScalingGroupsRequest autoScalingGroupsRequest) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.describeAutoScalingGroups(autoScalingGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeAutoScalingGroupsResult();
  }

  public Datapoint getCloudWatchMetricStatistics(
      AwsConfig awsConfig, String region, GetMetricStatisticsRequest metricStatisticsRequest) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      Datapoint datapoint = cloudWatchClient.getMetricStatistics(metricStatisticsRequest)
                                .getDatapoints()
                                .stream()
                                .max(Comparator.comparing(Datapoint::getTimestamp))
                                .orElse(null);
      return datapoint;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new Datapoint();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, String region) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return cloudWatchClient.listMetrics().getMetrics();

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, String region, ListMetricsRequest listMetricsRequest) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return cloudWatchClient.listMetrics(listMetricsRequest).getMetrics();

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public boolean registerInstancesWithLoadBalancer(
      Regions region, String accessKey, char[] secretKey, String loadBalancerName, String instanceId) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey);
      return elbClient
          .registerInstancesWithLoadBalancer(
              new RegisterInstancesWithLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .anyMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  public boolean deregisterInstancesFromLoadBalancer(
      Regions region, String accessKey, char[] secretKey, String loadBalancerName, String instanceId) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey);
      return elbClient
          .deregisterInstancesFromLoadBalancer(
              new DeregisterInstancesFromLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .noneMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  public CreateStackResult createStack(String accessKey, char[] secretKey, CreateStackRequest createStackRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient = getAmazonCloudFormationClient(accessKey, secretKey);
      return cloudFormationClient.createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateStackResult();
  }

  public DescribeStacksResult describeStacks(
      String accessKey, char[] secretKey, DescribeStacksRequest describeStacksRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient = getAmazonCloudFormationClient(accessKey, secretKey);
      return cloudFormationClient.describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeStacksResult();
  }

  public ListFunctionsResult listFunctions(
      String region, String accessKey, char[] secretKey, ListFunctionsRequest listFunctionsRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listFunctions(listFunctionsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListFunctionsResult();
  }

  public GetFunctionResult getFunction(
      String region, String accessKey, char[] secretKey, GetFunctionRequest getFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).getFunction(getFunctionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonServiceException instanceof ResourceNotFoundException) {
        return null;
      }
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetFunctionResult();
  }

  public ListVersionsByFunctionResult listVersionsByFunction(
      String region, String accessKey, char[] secretKey, ListVersionsByFunctionRequest listVersionsByFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listVersionsByFunction(listVersionsByFunctionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListVersionsByFunctionResult();
  }

  public CreateFunctionResult createFunction(
      String region, String accessKey, char[] secretKey, CreateFunctionRequest createFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).createFunction(createFunctionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateFunctionResult();
  }

  public UpdateFunctionCodeResult updateFunctionCode(
      String region, String accessKey, char[] secretKey, UpdateFunctionCodeRequest updateFunctionCodeRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).updateFunctionCode(updateFunctionCodeRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateFunctionCodeResult();
  }

  public UpdateFunctionConfigurationResult updateFunctionConfiguration(String region, String accessKey,
      char[] secretKey, UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey)
          .updateFunctionConfiguration(updateFunctionConfigurationRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateFunctionConfigurationResult();
  }

  public PublishVersionResult publishVersion(
      String region, String accessKey, char[] secretKey, PublishVersionRequest publishVersionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).publishVersion(publishVersionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new PublishVersionResult();
  }

  public ListAliasesResult listAliases(
      String region, String accessKey, char[] secretKey, ListAliasesRequest listAliasesRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listAliases(listAliasesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListAliasesResult();
  }

  public CreateAliasResult createAlias(
      String region, String accessKey, char[] secretKey, CreateAliasRequest createAliasRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).createAlias(createAliasRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAliasResult();
  }

  public UpdateAliasResult updateAlias(
      String region, String accessKey, char[] secretKey, UpdateAliasRequest updateAliasRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).updateAlias(updateAliasRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateAliasResult();
  }

  public InvokeResult invokeFunction(String region, String accessKey, char[] secretKey, InvokeRequest invokeRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).invoke(invokeRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new InvokeResult();
  }

  public boolean isVersioningEnabledForBucket(AwsConfig awsConfig, String bucketName) {
    try {
      BucketVersioningConfiguration bucketVersioningConfiguration =
          getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, bucketName))
              .getBucketVersioningConfiguration(bucketName);
      return "ENABLED".equals(bucketVersioningConfiguration.getStatus());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  private String getBucketRegion(AwsConfig awsConfig, String bucketName) {
    try {
      // You can query the bucket location using any region, it returns the result. So, using the default
      String region = getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1")
                          .getBucketLocation(bucketName);
      // Aws returns US if the bucket was created in the default region. Not sure why it doesn't return just the region
      // name in all cases. Also, their documentation says it would return empty string if its in the default region.
      // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGETlocation.html But it returns US. Added additional
      // checks based on other stuff
      if (region == "US" || region == null) {
        return "us-east-1";
      } else if (region == "EU") {
        return "eu-west-1";
      }

      return region;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }
}
