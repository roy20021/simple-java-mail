package org.simplejavamail.converter.internal.mimemessage;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jetbrains.annotations.NotNull;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.internal.moduleloader.ModuleLoader;
import org.simplejavamail.mailer.internal.util.MessageIdFixingMimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import static java.util.Optional.ofNullable;
import static org.simplejavamail.internal.util.MiscUtil.checkArgumentNotEmpty;
import static org.simplejavamail.internal.util.Preconditions.checkNonEmptyArgument;

/**
 * Helper class that produces and populates a mime messages. Deals with jakarta.mail RFC MimeMessage stuff, as well as
 * <a href="https://www.simplejavamail.org/#section-dkim">DKIM signing</a> and
 * <a href="https://www.simplejavamail.org/#section-sending-smime">S/MIME signing / encryption</a>.
 * <p>
 * Some more <a href="https://blogs.technet.microsoft.com/exchange/2011/04/21/mixed-ing-it-up-multipartmixed-messages-and-you/">helpful reading
 * material</a>.
 * <p>
 * One goal of these classes is to produce a Mime structure that exactly matches the email's need. Previously, Simple Java Mail
 * used a complex RFC-conforming structure that is compatible with all situations, but it seems
 * <a href="https://github.com/bbottema/simple-java-mail/issues/133">some email clients</a> would still get confused.
 * Due to this, we explicitely define all possible structures so we take the least complex one needed.
 *
 * @see <a href="https://github.com/bbottema/simple-java-mail/issues/144">#144: Simple Java Mail should tailor the MimeMessage structure to specific needs</a>
 */
public abstract class SpecializedMimeMessageProducer {
	
	/**
	 * @return Whether this mimemessage producer exactly matches the needs of the given email.
	 */
	abstract boolean compatibleWithEmail(@NotNull Email email);
	
	final MimeMessage populateMimeMessage(@NotNull final Email email, @NotNull Session session)
			throws MessagingException, UnsupportedEncodingException {
		checkArgumentNotEmpty(email, "email is missing");
		checkArgumentNotEmpty(session, "session is needed, it cannot be attached later");

		MimeMessage message = new MessageIdFixingMimeMessage(session, email.getId());
		
		// set basic email properties
		MimeMessageHelper.setSubject(email, message);
		MimeMessageHelper.setFrom(email, message);
		MimeMessageHelper.setReplyTo(email, message);
		MimeMessageHelper.setRecipients(email, message);
		
		populateMimeMessageMultipartStructure(message, email);
		
		MimeMessageHelper.setHeaders(email, message);
		message.setSentDate(ofNullable(email.getSentDate()).orElse(new Date()));

		/*
			The following order is important:
			1. S/MIME signing
			2. S/MIME encryption
			3. DKIM signing
		 */

		if (email.getPkcs12ConfigForSmimeSigning() != null) {
			message = ModuleLoader.loadSmimeModule().signMessageWithSmime(session, email, message, email.getPkcs12ConfigForSmimeSigning());
		}

		if (email.getX509CertificateForSmimeEncryption() != null) {
			message = ModuleLoader.loadSmimeModule().encryptMessageWithSmime(session, email, message, email.getX509CertificateForSmimeEncryption());
		}

		if (email.getDkimConfig() != null) {
			message = ModuleLoader.loadDKIMModule().signMessageWithDKIM(email, message, email.getDkimConfig(), checkNonEmptyArgument(email.getFromRecipient(), "fromRecipient"));
		}

		if (email.getBounceToRecipient() != null) {
			// display name not applicable: https://tools.ietf.org/html/rfc5321#section-4.1.2
			message = new ImmutableDelegatingSMTPMessage(message, email.getBounceToRecipient().getAddress());
		}

		return message;
	}

	abstract void populateMimeMessageMultipartStructure(MimeMessage  message, Email email) throws MessagingException;
	
	
	static boolean emailContainsMixedContent(@NotNull Email email) {
		return !email.getAttachments().isEmpty() || email.getEmailToForward() != null;
	}
	
	static boolean emailContainsRelatedContent(@NotNull Email email) {
		return !email.getEmbeddedImages().isEmpty();
	}
	
	static boolean emailContainsAlternativeContent(@NotNull Email email) {
		return (email.getPlainText() != null ? 1 : 0) +
				(email.getHTMLText() != null ? 1 : 0) +
				(email.getCalendarText() != null ? 1 : 0) > 1;
	}
}