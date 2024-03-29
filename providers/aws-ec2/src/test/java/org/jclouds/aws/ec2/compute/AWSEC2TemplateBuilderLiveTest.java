/**
 *
 * Copyright (C) 2011 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.jclouds.aws.ec2.compute;

import static org.jclouds.compute.util.ComputeServiceUtils.getCores;
import static org.jclouds.location.reference.LocationConstants.PROPERTY_REGIONS;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.aws.domain.Region;
import org.jclouds.aws.ec2.reference.AWSEC2Constants;
import org.jclouds.compute.BaseTemplateBuilderLiveTest;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.OsFamilyVersion64Bit;
import org.jclouds.compute.domain.Template;
import org.jclouds.ec2.compute.predicates.EC2ImagePredicates;
import org.jclouds.ec2.domain.InstanceType;
import org.jclouds.ec2.domain.RootDeviceType;
import org.jclouds.ec2.reference.EC2Constants;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

/**
 * 
 * @author Adrian Cole
 */
@Test(groups = "live")
public class AWSEC2TemplateBuilderLiveTest extends BaseTemplateBuilderLiveTest {

   public AWSEC2TemplateBuilderLiveTest() {
      provider = "aws-ec2";
   }

   @Override
   protected Predicate<OsFamilyVersion64Bit> defineUnsupportedOperatingSystems() {
      return Predicates.not(new Predicate<OsFamilyVersion64Bit>() {

         @Override
         public boolean apply(OsFamilyVersion64Bit input) {
            switch (input.family) {
               case UBUNTU:
                  return true;
               case DEBIAN:
                  return true;
               case CENTOS:
                  return input.version.matches("5.[42]") || input.version.equals("");
               case WINDOWS:
                  return input.version.matches("200[38]") || input.version.equals("");
               default:
                  return false;
            }
         }

      });
   }

   @Test
   public void testTemplateBuilderM1SMALLWithDescription() {

      Template template = context.getComputeService().templateBuilder().hardwareId(InstanceType.M1_SMALL)
               .osVersionMatches("1[10].[10][04]").imageDescriptionMatches("ubuntu-images").osFamily(OsFamily.UBUNTU)
               .build();

      assert (template.getImage().getProviderId().startsWith("ami-")) : template;
      assertEquals(template.getImage().getOperatingSystem().getVersion(), "11.10");
      assertEquals(template.getImage().getOperatingSystem().is64Bit(), false);
      assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.UBUNTU);
      assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
      assertEquals(template.getLocation().getId(), "us-east-1");
      assertEquals(getCores(template.getHardware()), 1.0d);
      assertEquals(template.getHardware().getId(), InstanceType.M1_SMALL);
      assertEquals(template.getImage().getOperatingSystem().getArch(), "paravirtual");
   }

   @Test
   public void testUbuntuInstanceStoreGoesM1Small() {

      Template template = context.getComputeService().templateBuilder()
            .imageMatches(EC2ImagePredicates.rootDeviceType(RootDeviceType.INSTANCE_STORE))
            .osVersionMatches("1[10].[10][04]").imageDescriptionMatches("ubuntu-images").osFamily(OsFamily.UBUNTU)
            .build();

      assert (template.getImage().getProviderId().startsWith("ami-")) : template;
      assertEquals(template.getImage().getOperatingSystem().getVersion(), "11.10");
      assertEquals(template.getImage().getOperatingSystem().is64Bit(), false);
      assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.UBUNTU);
      assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
      assertEquals(template.getLocation().getId(), "us-east-1");
      assertEquals(getCores(template.getHardware()), 1.0d);
      assertEquals(template.getHardware().getId(), InstanceType.M1_SMALL);
      assertEquals(template.getImage().getOperatingSystem().getArch(), "paravirtual");
   }
   @Test
   public void testTemplateBuilderCanUseImageIdAndhardwareIdAndAZ() {

      Template template = context.getComputeService().templateBuilder().imageId("us-east-1/ami-ccb35ea5").hardwareId(
               InstanceType.M2_2XLARGE).locationId("us-east-1a").build();

      System.out.println(template.getHardware());
      assert (template.getImage().getProviderId().startsWith("ami-")) : template;
      assertEquals(template.getImage().getOperatingSystem().getVersion(), "5.4");
      assertEquals(template.getImage().getOperatingSystem().is64Bit(), true);
      assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.CENTOS);
      assertEquals(template.getImage().getVersion(), "4.4.10");
      assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
      assertEquals(template.getLocation().getId(), "us-east-1a");
      assertEquals(template.getImage().getLocation().getId(), "us-east-1");
      assertEquals(getCores(template.getHardware()), 4.0d);
      assertEquals(template.getHardware().getId(), InstanceType.M2_2XLARGE);
      assertEquals(template.getImage().getOperatingSystem().getArch(), "paravirtual");
   }

   @Test
   public void testDefaultTemplateBuilder() throws IOException {

      Template defaultTemplate = context.getComputeService().templateBuilder().build();
      assert (defaultTemplate.getImage().getProviderId().startsWith("ami-")) : defaultTemplate;
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getVersion(), "2011.02.1");
      assertEquals(defaultTemplate.getImage().getOperatingSystem().is64Bit(), true);
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getFamily(), OsFamily.AMZN_LINUX);
      assertEquals(defaultTemplate.getImage().getUserMetadata().get("rootDeviceType"), "ebs");
      assertEquals(defaultTemplate.getLocation().getId(), "us-east-1");
      assertEquals(getCores(defaultTemplate.getHardware()), 1.0d);
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getArch(), "paravirtual");
   }


   @Test
   public void testAmazonLinuxInstanceStore() throws IOException {

      Template defaultTemplate = context.getComputeService().templateBuilder().osFamily(OsFamily.AMZN_LINUX)
            .imageMatches(EC2ImagePredicates.rootDeviceType(RootDeviceType.INSTANCE_STORE)).build();
      assert (defaultTemplate.getImage().getProviderId().startsWith("ami-")) : defaultTemplate;
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getVersion(), "2011.02.1");
      assertEquals(defaultTemplate.getImage().getOperatingSystem().is64Bit(), false);
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getFamily(), OsFamily.AMZN_LINUX);
      assertEquals(defaultTemplate.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
      assertEquals(defaultTemplate.getLocation().getId(), "us-east-1");
      assertEquals(getCores(defaultTemplate.getHardware()), 1.0d);
      assertEquals(defaultTemplate.getImage().getOperatingSystem().getArch(), "paravirtual");
   }

   @Test
   public void testFastestTemplateBuilder() throws IOException {
      Template fastestTemplate = context.getComputeService().templateBuilder().fastest().osFamily(OsFamily.AMZN_LINUX)
               .build();
      assert (fastestTemplate.getImage().getProviderId().startsWith("ami-")) : fastestTemplate;
      assertEquals(fastestTemplate.getHardware().getProviderId(), InstanceType.CC1_4XLARGE);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getVersion(), "2011.02.1-beta");
      assertEquals(fastestTemplate.getImage().getOperatingSystem().is64Bit(), true);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getFamily(), OsFamily.AMZN_LINUX);
      assertEquals(fastestTemplate.getImage().getUserMetadata().get("rootDeviceType"), "ebs");
      assertEquals(fastestTemplate.getLocation().getId(), "us-east-1");
      assertEquals(getCores(fastestTemplate.getHardware()), 8.0d);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getArch(), "hvm");

      fastestTemplate = context.getComputeService().templateBuilder().fastest().build();
      assert (fastestTemplate.getImage().getProviderId().startsWith("ami-")) : fastestTemplate;
      assertEquals(fastestTemplate.getHardware().getProviderId(), InstanceType.CC1_4XLARGE);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getVersion(), "11.10");
      assertEquals(fastestTemplate.getImage().getOperatingSystem().is64Bit(), true);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getFamily(), OsFamily.UBUNTU);
      assertEquals(fastestTemplate.getImage().getUserMetadata().get("rootDeviceType"), "ebs");
      assertEquals(fastestTemplate.getLocation().getId(), "us-east-1");
      assertEquals(getCores(fastestTemplate.getHardware()), 8.0d);
      assertEquals(fastestTemplate.getImage().getOperatingSystem().getArch(), "hvm");
   }

   @Test
   public void testTemplateBuilderMicro() throws IOException {

      Template microTemplate = context.getComputeService().templateBuilder().hardwareId(InstanceType.T1_MICRO)
               .osFamily(OsFamily.UBUNTU).osVersionMatches("10.10").os64Bit(true).build();

      assert (microTemplate.getImage().getProviderId().startsWith("ami-")) : microTemplate;
      assertEquals(microTemplate.getImage().getOperatingSystem().getVersion(), "10.10");
      assertEquals(microTemplate.getImage().getOperatingSystem().is64Bit(), true);
      assertEquals(microTemplate.getImage().getOperatingSystem().getFamily(), OsFamily.UBUNTU);
      assertEquals(microTemplate.getImage().getUserMetadata().get("rootDeviceType"), "ebs");
      assertEquals(microTemplate.getLocation().getId(), "us-east-1");
      assertEquals(getCores(microTemplate.getHardware()), 1.0d);
      assertEquals(microTemplate.getImage().getOperatingSystem().getArch(), "paravirtual");
   }

   @Test
   public void testTemplateBuilderWithNoOwnersParsesImageOnDemand() throws IOException {
      ComputeServiceContext context = null;
      try {
         Properties overrides = setupProperties();
         // set owners to nothing
         overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_AMI_QUERY, "");
         overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY, "");

         context = new ComputeServiceContextFactory().createContext(provider, ImmutableSet
                  .<Module> of(new Log4JLoggingModule()), overrides);

         assertEquals(context.getComputeService().listImages().size(), 0);

         Template template = context.getComputeService().templateBuilder().imageId("us-east-1/ami-ccb35ea5").build();
         System.out.println(template.getHardware());
         assert (template.getImage().getProviderId().startsWith("ami-")) : template;
         assertEquals(template.getImage().getOperatingSystem().getVersion(), "5.4");
         assertEquals(template.getImage().getOperatingSystem().is64Bit(), true);
         assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.CENTOS);
         assertEquals(template.getImage().getVersion(), "4.4.10");
         assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
         assertEquals(template.getLocation().getId(), "us-east-1");
         assertEquals(getCores(template.getHardware()), 2.0d);
         assertEquals(template.getHardware().getId(), "m1.large"); // because it
         // is 64bit

         // ensure we cache the new image for next time
         assertEquals(context.getComputeService().listImages().size(), 1);

      } finally {
         if (context != null)
            context.close();
      }
   }

   @Test
   public void testTemplateBuilderWithNoOwnersParsesImageOnDemandDeprecated() throws IOException {
      ComputeServiceContext context = null;
      try {
         Properties overrides = setupProperties();
         // set owners to nothing
         overrides.setProperty(AWSEC2Constants.PROPERTY_EC2_CC_AMIs, "");
         overrides.setProperty(EC2Constants.PROPERTY_EC2_AMI_OWNERS, "");

         context = new ComputeServiceContextFactory().createContext(provider, ImmutableSet
                  .<Module> of(new Log4JLoggingModule()), overrides);

         assertEquals(context.getComputeService().listImages().size(), 0);

         Template template = context.getComputeService().templateBuilder().imageId("us-east-1/ami-ccb35ea5").build();
         System.out.println(template.getHardware());
         assert (template.getImage().getProviderId().startsWith("ami-")) : template;
         assertEquals(template.getImage().getOperatingSystem().getVersion(), "5.4");
         assertEquals(template.getImage().getOperatingSystem().is64Bit(), true);
         assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.CENTOS);
         assertEquals(template.getImage().getVersion(), "4.4.10");
         assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
         assertEquals(template.getLocation().getId(), "us-east-1");
         assertEquals(getCores(template.getHardware()), 2.0d);
         assertEquals(template.getHardware().getId(), "m1.large"); // because it
         // is 64bit

         // ensure we cache the new image for next time
         assertEquals(context.getComputeService().listImages().size(), 1);

      } finally {
         if (context != null)
            context.close();
      }
   }

   @Test
   public void testTemplateBuilderWithLessRegions() throws IOException {
      ComputeServiceContext context = null;
      try {
         Properties overrides = setupProperties();
         // set regions to only 1
         overrides.setProperty(PROPERTY_REGIONS, Region.US_EAST_1);

         context = new ComputeServiceContextFactory().createContext(provider, ImmutableSet
                  .<Module> of(new Log4JLoggingModule()), overrides);

         assert context.getComputeService().listImages().size() < this.context.getComputeService().listImages().size();

         Template template = context.getComputeService().templateBuilder().imageId("us-east-1/ami-ccb35ea5").build();
         System.out.println(template.getHardware());
         assert (template.getImage().getProviderId().startsWith("ami-")) : template;
         assertEquals(template.getImage().getOperatingSystem().getVersion(), "5.4");
         assertEquals(template.getImage().getOperatingSystem().is64Bit(), true);
         assertEquals(template.getImage().getOperatingSystem().getFamily(), OsFamily.CENTOS);
         assertEquals(template.getImage().getVersion(), "4.4.10");
         assertEquals(template.getImage().getUserMetadata().get("rootDeviceType"), "instance-store");
         assertEquals(template.getLocation().getId(), "us-east-1");
         assertEquals(getCores(template.getHardware()), 2.0d);
         assertEquals(template.getHardware().getId(), "m1.large"); // because it is 64bit

      } finally {
         if (context != null)
            context.close();
      }
   }

   @Override
   protected Set<String> getIso3166Codes() {
      return ImmutableSet.<String> of("US-VA", "US-CA", "IE", "SG", "JP-13");
   }

}
