/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PlainSaslServerTest{
  private static byte sSEPARATOR = 0x00; // US-ASCII <NUL>
  private PlainSaslServer mPlainSaslServer = null;

  @Before
  public void before() throws Exception {
    mPlainSaslServer = new PlainSaslServer(new MockCallbackHandler());
  }

  /**
   * Simulate the authentication data included user information from client side
   * @param user The name of the user will be transferred from client side
   * @param password The password will be transferred from client side
   * @return The response is simulated from the client side
   */
  private byte[] getUserInfo(String user, String password) throws Exception {
    byte[] auth = user.getBytes("UTF8");
    byte[] pw = password.getBytes("UTF8");
    byte[] result = new byte[pw.length + auth.length + 2];
    int pos = 0;
    result[pos ++] = sSEPARATOR;
    System.arraycopy(auth, 0, result, pos, auth.length);

    pos += auth.length;
    result[pos ++] = sSEPARATOR;

    System.arraycopy(pw, 0, result, pos, pw.length);
    return result;
  }

  @Test
  public void testUserIsNotSet() throws Exception {
    try {
      mPlainSaslServer.evaluateResponse(getUserInfo("", "anonymous"));
      Assert.fail("expected exception has been happend");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("No authentication identity provided"));
    }
  }

  @Test
  public void testPasswordIsNotSet() throws Exception {
    try {
      mPlainSaslServer.evaluateResponse(getUserInfo("tachyon", ""));
      Assert.fail("expected exception has been happend");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("No password provided"));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testAuthenticationHasNotComplete() throws Exception {
    mPlainSaslServer.getAuthorizationID();
  }

  @Test
  public void testUserPasswordReceive() throws Exception {
    String testUser = "tachyon";
    String password = "anonymous";
    mPlainSaslServer.evaluateResponse(getUserInfo(testUser, password));
    Assert.assertEquals(testUser, mPlainSaslServer.getAuthorizationID());
  }

  private static class MockCallbackHandler implements CallbackHandler {
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      AuthorizeCallback ac = null;
      for (Callback callback : callbacks) {
        if (callback instanceof AuthorizeCallback) {
          ac = (AuthorizeCallback)callback;
        }
      }
      ac.setAuthorized(true);
    }
  }
}
