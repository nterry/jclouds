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
package org.jclouds.trmk.vcloudexpress;

import static org.jclouds.trmk.vcloud_0_8.options.InstantiateVAppTemplateOptions.Builder.processorCount;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jclouds.domain.Credentials;
import org.jclouds.net.IPSocket;
import org.jclouds.ssh.SshClient;
import org.jclouds.trmk.vcloud_0_8.TerremarkClientLiveTest;
import org.jclouds.trmk.vcloud_0_8.domain.InternetService;
import org.jclouds.trmk.vcloud_0_8.domain.KeyPair;
import org.jclouds.trmk.vcloud_0_8.domain.Protocol;
import org.jclouds.trmk.vcloud_0_8.domain.PublicIpAddress;
import org.jclouds.trmk.vcloud_0_8.domain.Org;
import org.jclouds.trmk.vcloud_0_8.domain.VApp;
import org.jclouds.trmk.vcloud_0_8.options.InstantiateVAppTemplateOptions;
import org.jclouds.trmk.vcloudexpress.suppliers.TerremarkVCloudExpressInternetServiceAndPublicIpAddressSupplier;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

/**
 * Tests behavior of {@code TerremarkVCloudExpressClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", singleThreaded = true, testName = "TerremarkVCloudExpressClientLiveTest")
public class TerremarkVCloudExpressClientLiveTest extends TerremarkClientLiveTest {

   KeyPair key;

   @Override
   protected void prepare() {
      TerremarkVCloudExpressClient vCloudExpressClient = TerremarkVCloudExpressClient.class.cast(connection);

      Org org = vCloudExpressClient.findOrgNamed(null);
      try {
         key = vCloudExpressClient.generateKeyPairInOrg(org.getHref(), "livetest", false);
      } catch (IllegalStateException e) {
         key = vCloudExpressClient.findKeyPairInOrg(org.getHref(), "livetest");
         vCloudExpressClient.deleteKeyPair(key.getId());
         key = vCloudExpressClient.generateKeyPairInOrg(org.getHref(), "livetest", false);
      }
      assertNotNull(key);
      assertEquals(key.getName(), "livetest");
      assertNotNull(key.getPrivateKey());
      assertNotNull(key.getFingerPrint());
      assertEquals(key.isDefault(), false);
      assertEquals(key.getFingerPrint(), vCloudExpressClient.findKeyPairInOrg(org.getHref(), key.getName())
            .getFingerPrint());
   }

   @AfterTest
   void cleanup1() throws InterruptedException, ExecutionException, TimeoutException {
      if (key != null) {
         TerremarkVCloudExpressClient vCloudExpressClient = TerremarkVCloudExpressClient.class.cast(connection);
         vCloudExpressClient.deleteKeyPair(key.getId());
      }
   }

   @Override
   protected SshClient getConnectionFor(IPSocket socket) {
      return sshFactory.create(socket, new Credentials("vcloud", key.getPrivateKey()));
   }

   @Override
   protected InstantiateVAppTemplateOptions createInstantiateOptions() {
      return processorCount(1).memory(512).sshKeyFingerprint(key.getFingerPrint());
   }

   @Override
   protected Entry<InternetService, PublicIpAddress> getNewInternetServiceAndIpForSSH(VApp vApp) {
      return new TerremarkVCloudExpressInternetServiceAndPublicIpAddressSupplier(
            TerremarkVCloudExpressClient.class.cast(connection)).getNewInternetServiceAndIp(vApp, 22, Protocol.TCP);
   }
}
