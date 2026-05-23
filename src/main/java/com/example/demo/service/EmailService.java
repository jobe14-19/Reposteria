package com.example.demo.service;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

 private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

 private static final String SMTP_HOST = "smtp.gmail.com";
 private static final String SMTP_PORT = "587";
 private static final String USERNAME = "reposteria.rosato.notificaciones@gmail.com";
 private static final String PASSWORD = "tu_contraseña_aqui";

 private final Session session;

 public EmailService() {
 Properties props = new Properties();
 props.put("mail.smtp.auth", "true");
 props.put("mail.smtp.starttls.enable", "true");
 props.put("mail.smtp.host", SMTP_HOST);
 props.put("mail.smtp.port", SMTP_PORT);

 session = Session.getInstance(props, new javax.mail.Authenticator() {
 @Override
 protected PasswordAuthentication getPasswordAuthentication() {
 return new PasswordAuthentication(USERNAME, PASSWORD);
 }
 });
 }

 public void enviarCorreo(String destinatario, String asunto, String cuerpo) {
 try {
 MimeMessage message = new MimeMessage(session);
 message.setFrom(new InternetAddress(USERNAME));
 message.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
 message.setSubject(asunto);
 message.setContent(cuerpo, "text/html; charset=utf-8");
 Transport.send(message);
 LOGGER.log(Level.INFO, "Correo enviado a {0}: {1}", new Object[]{destinatario, asunto});
 } catch (MessagingException e) {
 LOGGER.log(Level.SEVERE, "Error al enviar correo a {0}: {1}", new Object[]{destinatario, e.getMessage()});
 }
 }

 public void notificarPedidoConfirmado(String destinatario, int idPedido, String cliente, double total) {
 String asunto = "Pedido #" + idPedido + " Confirmado - Repostería Rosato";
 String cuerpo = String.format("""
 <html>
 <body style="font-family: Arial; padding: 20px;">
 <h2 style="color: #8B5E3C;">Repostería Rosato</h2>
 <p>Hola <b>%s</b>,</p>
 <p>Tu pedido <b>#%d</b> ha sido confirmado.</p>
 <p><b>Total:</b> RD$ %.2f</p>
 <hr>
 <p style="font-size: 12px; color: #888;">Gracias por preferirnos.</p>
 </body>
 </html>
 """, cliente, idPedido, total);
 enviarCorreo(destinatario, asunto, cuerpo);
 }

 public void notificarFactura(String destinatario, int idPedido, String cliente, double total, double adelanto, double saldo) {
 String asunto = "Factura Pedido #" + idPedido + " - Repostería Rosato";
 String cuerpo = String.format("""
 <html>
 <body style="font-family: Arial; padding: 20px;">
 <h2 style="color: #8B5E3C;">Repostería Rosato</h2>
 <p>Hola <b>%s</b>,</p>
 <p>Adjuntamos la factura de tu pedido <b>#%d</b>.</p>
 <table border="1" cellpadding="8" style="border-collapse: collapse;">
 <tr><td><b>Total</b></td><td>RD$ %.2f</td></tr>
 <tr><td><b>Adelanto</b></td><td>RD$ %.2f</td></tr>
 <tr><td><b>Saldo</b></td><td>RD$ %.2f</td></tr>
 </table>
 <hr>
 <p style="font-size: 12px; color: #888;">Gracias por preferirnos.</p>
 </body>
 </html>
 """, cliente, idPedido, total, adelanto, saldo);
 enviarCorreo(destinatario, asunto, cuerpo);
 }
}
