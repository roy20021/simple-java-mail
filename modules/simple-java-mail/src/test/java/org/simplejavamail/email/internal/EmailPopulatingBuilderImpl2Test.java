package org.simplejavamail.email.internal;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailAssert;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.api.email.config.DkimConfig;
import org.simplejavamail.config.ConfigLoader.Property;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.internal.util.CertificationUtil;
import testutil.ConfigLoaderTestHelper;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.function.Function;

import static demo.ResourceFolderHelper.determineResourceFolder;
import static jakarta.mail.Message.RecipientType.BCC;
import static jakarta.mail.Message.RecipientType.CC;
import static jakarta.mail.Message.RecipientType.TO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.simplejavamail.api.email.ContentTransferEncoding.BASE_64;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_BCC_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_BCC_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_BOUNCETO_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_BOUNCETO_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_CC_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_CC_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_CONTENT_TRANSFER_ENCODING;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_FROM_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_FROM_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_REPLYTO_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_REPLYTO_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_SUBJECT;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_TO_ADDRESS;
import static org.simplejavamail.config.ConfigLoader.Property.DEFAULT_TO_NAME;
import static org.simplejavamail.config.ConfigLoader.Property.DKIM_EXCLUDED_HEADERS_FROM_DEFAULT_SIGNING_LIST;
import static org.simplejavamail.config.ConfigLoader.Property.DKIM_PRIVATE_KEY_FILE_OR_DATA;
import static org.simplejavamail.config.ConfigLoader.Property.DKIM_SELECTOR;
import static org.simplejavamail.config.ConfigLoader.Property.DKIM_SIGNING_DOMAIN;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_CLASSPATH;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_DIR;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_URL;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_CLASSPATH;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_DIR;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_URL;
import static org.simplejavamail.config.ConfigLoader.Property.EMBEDDEDIMAGES_DYNAMICRESOLUTION_MUSTBESUCCESFUL;
import static org.simplejavamail.config.ConfigLoader.Property.SMIME_ENCRYPTION_CERTIFICATE;
import static org.simplejavamail.config.ConfigLoader.Property.SMIME_SIGNING_KEYSTORE;
import static org.simplejavamail.config.ConfigLoader.Property.SMIME_SIGNING_KEYSTORE_PASSWORD;
import static org.simplejavamail.config.ConfigLoader.Property.SMIME_SIGNING_KEY_ALIAS;
import static org.simplejavamail.config.ConfigLoader.Property.SMIME_SIGNING_KEY_PASSWORD;
import static org.simplejavamail.mailer.internal.EmailGovernanceImpl.NO_GOVERNANCE;
import static org.simplejavamail.util.TestDataHelper.getUrl;
import static org.simplejavamail.util.TestDataHelper.loadPkcs12KeyStore;

public class EmailPopulatingBuilderImpl2Test {

	private static final String RESOURCES_PATH = determineResourceFolder("simple-java-mail") + "/test/resources";

	private static final String DOWNLOAD_SIMPLE_JAVA_MAIL = "Download Simple Java Mail";
	private static final String CREATE_SELF_SIGNED_S_MIME_CERTIFICATES = "Create Self-Signed S/MIME Certificates";
	private static final String CONSOLE_NAME_CONSOLE_TARGET_SYSTEM_OUT = "<Console name=\"console\" target=\"SYSTEM_OUT\">";

	@Test
	public void testConstructorApplyingPreconfiguredDefaults1() throws Exception {
		Map<Property, Object> value = new HashedMap<>();
		value.put(DEFAULT_FROM_ADDRESS, "test_from@domain.com");
		value.put(DEFAULT_FROM_NAME, "Test From");
		value.put(DEFAULT_REPLYTO_ADDRESS, "test_replyto@domain.com");
		value.put(DEFAULT_REPLYTO_NAME, "Test Replyto");
		value.put(DEFAULT_BOUNCETO_ADDRESS, "test_boundeto@domain.com");
		value.put(DEFAULT_BOUNCETO_NAME, "Test Bounceto");
		value.put(DEFAULT_TO_ADDRESS, "test_to1@domain.com,test_to2@domain.com");
		value.put(DEFAULT_TO_NAME, "test TO name");
		value.put(DEFAULT_CC_ADDRESS, "test_cc1@domain.com,test_cc2@domain.com");
		value.put(DEFAULT_CC_NAME, "test CC name");
		value.put(DEFAULT_BCC_ADDRESS, "test_bcc1@domain.com,test_bcc2@domain.com");
		value.put(DEFAULT_BCC_NAME, "test BCC name");
		value.put(DEFAULT_SUBJECT, "test subject");
		value.put(DEFAULT_CONTENT_TRANSFER_ENCODING, BASE_64);
		value.put(SMIME_SIGNING_KEYSTORE, "src/test/resources/pkcs12/smime_keystore.pkcs12");
		value.put(SMIME_SIGNING_KEYSTORE_PASSWORD, "letmein");
		value.put(SMIME_SIGNING_KEY_ALIAS, "smime_test_user_alias");
		value.put(SMIME_SIGNING_KEY_PASSWORD, "letmein");
		value.put(SMIME_ENCRYPTION_CERTIFICATE, "src/test/resources/pkcs12/smime_test_user.pem.standard.crt");
		value.put(DKIM_PRIVATE_KEY_FILE_OR_DATA, "src/test/resources/dkim/dkim_dummy_key.der");
		value.put(DKIM_SIGNING_DOMAIN, "ignore.com");
		value.put(DKIM_SELECTOR, "ignore");
		value.put(DKIM_EXCLUDED_HEADERS_FROM_DEFAULT_SIGNING_LIST, "Ignored");

		ConfigLoaderTestHelper.setResolvedProperties(value);

		EmailAssert.assertThat(EmailBuilder.startingBlank().buildEmail())
				.hasFromRecipient(null)
				.hasNoReplyToRecipients()
				.hasBounceToRecipient(null)
				.hasNoRecipients()
				.hasSubject(null)
				.hasPkcs12ConfigForSmimeSigning(null)
				.hasX509CertificateForSmimeEncryption(null)
				.hasDkimConfig(null);

		EmailAssert.assertThat(EmailBuilder.startingBlank().buildEmailCompletedWithDefaultsAndOverrides())
				.hasFromRecipient(new Recipient("Test From", "test_from@domain.com", null))
				.hasReplyToRecipients(new Recipient("Test Replyto", "test_replyto@domain.com", null))
				.hasBounceToRecipient(new Recipient("Test Bounceto", "test_boundeto@domain.com", null))
				.hasRecipients(
						new Recipient("test TO name", "test_to1@domain.com", TO), new Recipient("test TO name", "test_to2@domain.com", TO),
						new Recipient("test CC name", "test_cc1@domain.com", CC), new Recipient("test CC name", "test_cc2@domain.com", CC),
						new Recipient("test BCC name", "test_bcc1@domain.com", BCC), new Recipient("test BCC name", "test_bcc2@domain.com", BCC)
				)
				.hasSubject("test subject")
				.hasPkcs12ConfigForSmimeSigning(loadPkcs12KeyStore())
				.hasX509CertificateForSmimeEncryption(CertificationUtil.readFromPem(new File(RESOURCES_PATH + "/pkcs12/smime_test_user.pem.standard.crt")))
				.hasDkimConfig(DkimConfig.builder()
						.dkimPrivateKeyPath(RESOURCES_PATH + "/dkim/dkim_dummy_key.der")
						.dkimSigningDomain("ignore.com")
						.dkimSelector("ignore")
						.excludedHeadersFromDkimDefaultSigningList("Ignored")
						.build());
	}

	@Test
	public void testConstructorApplyingPreconfiguredDefaults2() throws Exception {
		HashedMap<Property, Object> value = new HashedMap<>();
		value.put(DEFAULT_FROM_ADDRESS, "test_from@domain.com");
		value.put(DEFAULT_REPLYTO_ADDRESS, "test_replyto@domain.com");
		value.put(DEFAULT_BOUNCETO_ADDRESS, "test_boundeto@domain.com");
		value.put(DEFAULT_TO_ADDRESS, "test_to1@domain.com,test_to2@domain.com");
		value.put(DEFAULT_CC_ADDRESS, "test_cc1@domain.com,test_cc2@domain.com");
		value.put(DEFAULT_BCC_ADDRESS, "test_bcc1@domain.com,test_bcc2@domain.com");
		value.put(SMIME_ENCRYPTION_CERTIFICATE, "src/test/resources/pkcs12/smime_test_user.pem.standard.crt");

		ConfigLoaderTestHelper.setResolvedProperties(value);

		EmailAssert.assertThat(EmailBuilder.startingBlank().buildEmail())
				.hasFromRecipient(null)
				.hasNoReplyToRecipients()
				.hasBounceToRecipient(null)
				.hasNoRecipients()
				.hasX509CertificateForSmimeEncryption(null);

		assertThat(NO_GOVERNANCE().produceEmailApplyingDefaultsAndOverrides(null).getX509CertificateForSmimeEncryption())
				.isEqualTo(CertificationUtil.readFromPem(new File(RESOURCES_PATH + "/pkcs12/smime_test_user.pem.standard.crt")));

		EmailAssert.assertThat(EmailBuilder.startingBlank().buildEmailCompletedWithDefaultsAndOverrides())
				.hasFromRecipient(new Recipient(null, "test_from@domain.com", null))
				.hasReplyToRecipients(new Recipient(null, "test_replyto@domain.com", null))
				.hasBounceToRecipient(new Recipient(null, "test_boundeto@domain.com", null))
				.hasRecipients(
						new Recipient(null, "test_to1@domain.com", TO), new Recipient(null, "test_to2@domain.com", TO),
						new Recipient(null, "test_cc1@domain.com", CC), new Recipient(null, "test_cc2@domain.com", CC),
						new Recipient(null, "test_bcc1@domain.com", BCC), new Recipient(null, "test_bcc2@domain.com", BCC)
				)
				.hasX509CertificateForSmimeEncryption(CertificationUtil.readFromPem(new File(RESOURCES_PATH + "/pkcs12/smime_test_user.pem.standard.crt")));
	}

	@Test
	public void testConstructorApplyingPreconfiguredDefaults_EmbeddedImageResolving() throws Exception {
		assumeThat(getUrl("https://www.simplejavamail.org")).isEqualTo(HttpURLConnection.HTTP_OK);

		HashedMap<Property, Object> value = new HashedMap<>();

		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_DIR, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_CLASSPATH, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_URL, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_DIR, RESOURCES_PATH);
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_URL, "https://www.simplejavamail.org");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_CLASSPATH, "/pkcs12");

		ConfigLoaderTestHelper.setResolvedProperties(value);

		Email email = EmailBuilder.startingBlank()
				.withHTMLText("<img src=\"cid:cid_name\"/>")
				.appendTextHTML("<img src=\"download.html\"/>") // comes from simplejavamail.org
				.appendTextHTML("<img src=\"/how-to.html\"/>") // comes from classpath
				.appendTextHTML("<img src=\"log4j2.xml\"/>") // comes from folder
				.buildEmail();

		assertThat(email.getEmbeddedImages())
				.extracting(new DatasourceReadingExtractor())
				.containsExactlyInAnyOrder(
						DOWNLOAD_SIMPLE_JAVA_MAIL,
						CREATE_SELF_SIGNED_S_MIME_CERTIFICATES,
						CONSOLE_NAME_CONSOLE_TARGET_SYSTEM_OUT
				);
	}

	@Test
	public void testConstructorApplyingPreconfiguredDefaults_EmbeddedImageResolving_BubbleFailure() throws Exception {
		assumeThat(getUrl("https://www.simplejavamail.org")).isEqualTo(HttpURLConnection.HTTP_OK);

		HashedMap<Property, Object> value = new HashedMap<>();

		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_DIR, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_CLASSPATH, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_URL, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_DIR, RESOURCES_PATH);
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_URL, "https://www.simplejavamail.org");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_CLASSPATH, "/pkcs12");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_MUSTBESUCCESFUL, "true");

		ConfigLoaderTestHelper.setResolvedProperties(value);

		final EmailPopulatingBuilder emailPopulatingBuilder = EmailBuilder.startingBlank()
				.withHTMLText("<img src=\"cid:cid_name\"/>")
				.appendTextHTML("<img src=\"missing.html\"/>");

		assertThatThrownBy(emailPopulatingBuilder::buildEmail)
				.isInstanceOf(EmailException.class)
				.hasMessage("Unable to dynamically resolve data source for the following image src: missing.html");
	}

	@Test
	public void testConstructorApplyingPreconfiguredDefaults_EmbeddedImageResolving_IgnoreFailure() throws Exception {
		assumeThat(getUrl("https://www.simplejavamail.org")).isEqualTo(HttpURLConnection.HTTP_OK);

		HashedMap<Property, Object> value = new HashedMap<>();

		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_DIR, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_CLASSPATH, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_ENABLE_URL, "true");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_DIR, RESOURCES_PATH);
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_URL, "https://www.simplejavamail.org");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_BASE_CLASSPATH, "/pkcs12");
		value.put(EMBEDDEDIMAGES_DYNAMICRESOLUTION_MUSTBESUCCESFUL, "false");

		ConfigLoaderTestHelper.setResolvedProperties(value);

		final Email email = EmailBuilder.startingBlank()
				.withHTMLText("<img src=\"cid:cid_name\"/>")
				.appendTextHTML("<img src=\"missing.html\"/>")
				.appendTextHTML("<img src=\"/missing.html\"/>")
				.appendTextHTML("<img src=\"missing.xml\"/>")
				.buildEmail();

		assertThat(email.getEmbeddedImages()).isEmpty();
	}

	private static class DatasourceReadingExtractor implements Function<AttachmentResource, String> {
		@Override
		public String apply(final AttachmentResource input) {
			try {
				final String sourceContent = input.readAllData();
				if (sourceContent.contains(DOWNLOAD_SIMPLE_JAVA_MAIL)) {
					return DOWNLOAD_SIMPLE_JAVA_MAIL;
				} else if (sourceContent.contains(CREATE_SELF_SIGNED_S_MIME_CERTIFICATES)) {
					return CREATE_SELF_SIGNED_S_MIME_CERTIFICATES;
				} else if (sourceContent.contains(CONSOLE_NAME_CONSOLE_TARGET_SYSTEM_OUT)) {
					return CONSOLE_NAME_CONSOLE_TARGET_SYSTEM_OUT;
				}
				return "";
			} catch (IOException e) {
				throw new AssertionError();
			}
		}
	}
}