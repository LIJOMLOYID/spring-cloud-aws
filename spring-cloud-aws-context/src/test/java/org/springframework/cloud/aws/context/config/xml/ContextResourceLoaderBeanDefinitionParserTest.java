/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.config.xml;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
public class ContextResourceLoaderBeanDefinitionParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parseInternal_defaultConfiguration_createsAmazonS3ClientWithoutRegionConfigured() {
        //Arrange
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

        //Act
        ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoaderBean.class).getResourceLoader();

        //Assert
        assertTrue(DefaultResourceLoader.class.isInstance(resourceLoader));

        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) resourceLoader;
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));

    }

    @Test
    public void parseInternal_configurationWithRegion_createsAmazonS3ClientWithRegionConfigured() {
        //Arrange
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withRegionConfigured.xml", getClass());

        //Act
        ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoaderBean.class).getResourceLoader();
        AmazonS3Client webServiceClient = applicationContext.getBean(AmazonS3Client.class);

        //Assert
        assertEquals(Region.getRegion(Regions.EU_WEST_1), webServiceClient.getRegion().toAWSRegion());

        assertTrue(DefaultResourceLoader.class.isInstance(resourceLoader));
        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) resourceLoader;
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    @Test
    public void parseInternal_configurationWithCustomRegionProvider_createsAmazonS3ClientWithRegionConfigured() {
        //Arrange
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withCustomRegionProvider.xml", getClass());

        //Act
        ResourceLoader resourceLoader = applicationContext.getBean(ResourceLoaderBean.class).getResourceLoader();
        AmazonS3Client webServiceClient = applicationContext.getBean(AmazonS3Client.class);

        //Assert
        assertEquals(Region.getRegion(Regions.US_WEST_2), webServiceClient.getRegion().toAWSRegion());

        assertTrue(DefaultResourceLoader.class.isInstance(resourceLoader));
        DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) resourceLoader;
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(defaultResourceLoader.getProtocolResolvers().iterator().next()));
    }

    @Test
    public void parseInternal_configurationWithCustomTaskExecutor_createsResourceLoaderWithCustomTaskExecutor() {
        //Arrange
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withCustomTaskExecutor.xml", getClass());

        //Act

        //Assert
        assertTrue(DefaultResourceLoader.class.isInstance(applicationContext));
        ProtocolResolver protocolResolver = applicationContext.getProtocolResolvers().iterator().next();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(protocolResolver));

        assertSame(applicationContext.getBean("taskExecutor"), ReflectionTestUtils.getField(protocolResolver, "taskExecutor"));
    }

    @Test
    public void parseInternal_configurationWithCustomAmazonS3Client_createResourceLoaderWithCustomS3Client() throws Exception {
        //Arrange
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-withCustomS3Client.xml", getClass());

        //Act
        assertTrue(DefaultResourceLoader.class.isInstance(applicationContext));
        ProtocolResolver protocolResolver = applicationContext.getProtocolResolvers().iterator().next();
        assertTrue(SimpleStorageProtocolResolver.class.isInstance(protocolResolver));

        //Assert that the proxied AmazonS2 instances are the same as the customS3Client in the app context.
        AmazonS3 customS3Client = applicationContext.getBean(AmazonS3.class);

        SimpleStorageProtocolResolver resourceLoader = (SimpleStorageProtocolResolver) protocolResolver;
        AmazonS3 amazonS3FromResourceLoader = (AmazonS3) ReflectionTestUtils.getField(resourceLoader, "amazonS3");

        assertThat(AopUtils.isAopProxy(amazonS3FromResourceLoader), is(true));

        Advised advised2 = (Advised) amazonS3FromResourceLoader;
        AmazonS3 amazonS3WrappedInsideSimpleStorageResourceLoader = (AmazonS3) advised2.getTargetSource().getTarget();

        assertSame(customS3Client, amazonS3WrappedInsideSimpleStorageResourceLoader);
    }


    static class ResourceLoaderBean implements ResourceLoaderAware {

        private ResourceLoader resourceLoader;

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        public ResourceLoader getResourceLoader() {
            return this.resourceLoader;
        }
    }
}
