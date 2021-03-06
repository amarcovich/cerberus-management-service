/*
 * Copyright (c) 2017 Nike, Inc.
 *
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
 *
 */

package com.nike.cerberus.util;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the AwsIamRoleArnParser class
 */
public class AwsIamRoleArnParserTest {

    private AwsIamRoleArnParser awsIamRoleArnParser;

    @Before
    public void setup() {

        awsIamRoleArnParser = new AwsIamRoleArnParser();
    }

    @Test
    public void getAccountId_returns_an_account_id_given_a_valid_arn() {

        assertEquals("1111111111", awsIamRoleArnParser.getAccountId("arn:aws:iam::1111111111:role/lamb_dev_health"));
    }

    @Test(expected = RuntimeException.class)
    public void getAccountId_fails_on_invalid_arn() {

        awsIamRoleArnParser.getAccountId("hullabaloo");
    }

    @Test
    public void getRoleNameHappy_returns_the_role_name_given_a_valid_arn() {

        assertEquals("my_roleName", awsIamRoleArnParser.getRoleName("arn:aws:iam::222222:role/my_roleName"));
    }

    @Test(expected = RuntimeException.class)
    public void getRoleName_fails_on_invalid_arn() {

        awsIamRoleArnParser.getRoleName("brouhaha");
    }

    @Test
    public void convertPrincipalArnToRoleArn_properly_converts_principals_to_role_arns() {

        assertEquals("arn:aws:iam::1111111111:role/lamb_dev_health", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::1111111111:federated-user/lamb_dev_health"));
        assertEquals("arn:aws:iam::2222222222:role/prince_role", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::2222222222:assumed-role/prince_role/session-name"));
        assertEquals("arn:aws:iam::2222222222:role/sir/alfred/role", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::2222222222:assumed-role/sir/alfred/role/session-name"));
        assertEquals("arn:aws:iam::3333333333:role/path/to/foo", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::3333333333:role/path/to/foo"));
        assertEquals("arn:aws:iam::4444444444:role/name", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::4444444444:role/name"));
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_invalid_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("foobar");
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_group_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::1111111111:group/path/to/group");
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_invalid_assumed_role_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::1111111111:assumed-role/blah");
    }

    @Test
    public void isRoleArn_returns_true_when_is_role_arn()  {

        assertTrue(awsIamRoleArnParser.isRoleArn("arn:aws:iam::2222222222:role/fancy/role/path"));
        assertTrue(awsIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:role/name"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:iam::3333333333:assumed-role/happy/path"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:sts::1111111111:federated-user/my_user"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:group/path/to/group"));
    }
}