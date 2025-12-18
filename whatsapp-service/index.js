const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('@whiskeysockets/baileys');
const qrcode = require('qrcode-terminal');
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const pino = require('pino');

const app = express();
app.use(cors());
app.use(bodyParser.json());

let sock;

async function connectToWhatsApp() {
    const { state, saveCreds } = await useMultiFileAuthState('auth_info_baileys');

    sock = makeWASocket({
        auth: state,
        printQRInTerminal: true,
        logger: pino({ level: 'silent' }) // 'info' for debug, 'silent' for clean output
    });

    sock.ev.on('creds.update', saveCreds);

    sock.ev.on('connection.update', (update) => {
        const { connection, lastDisconnect, qr } = update;

        if (qr) {
            console.log('Scan the QR code below to login:');
            qrcode.generate(qr, { small: true });
        }

        if (connection === 'close') {
            const shouldReconnect = (lastDisconnect.error)?.output?.statusCode !== DisconnectReason.loggedOut;
            console.log('Connection closed due to ', lastDisconnect.error, ', reconnecting ', shouldReconnect);
            if (shouldReconnect) {
                // Delay slightly before reconnecting
                setTimeout(connectToWhatsApp, 2000);
            }
        } else if (connection === 'open') {
            console.log('WhatsApp connection opened successfully!');
        }
    });
}

connectToWhatsApp();

app.post('/api/send-message', async (req, res) => {
    console.log('API Request:', JSON.stringify(req.body, null, 2));
    const { phone, message, imageUrl, buttons, poll } = req.body;

    if (!sock) {
        return res.status(503).json({ success: false, error: 'WhatsApp client not ready' });
    }

    if (!phone || (!message && !imageUrl && !poll)) {
        return res.status(400).json({ success: false, error: 'Missing phone or message/image/poll' });
    }

    try {
        // Format phone: remove non-digits, ensure country code presence (assuming user passes full number or we default)
        // For safety, assume user passes "919999999999" format. 
        // Appending @s.whatsapp.net
        let formattedPhone = phone.replace(/[^0-9]/g, '');

        // Basic check for country code length (very rough)
        if (formattedPhone.length < 10) {
            return res.status(400).json({ success: false, error: 'Invalid phone number' });
        }

        const jid = formattedPhone + '@s.whatsapp.net';

        // Prepare Buttons if any
        let msgButtons = [];
        if (buttons && Array.isArray(buttons)) {
            msgButtons = buttons.map((btn) => ({
                buttonId: btn.id,
                buttonText: { displayText: btn.text },
                type: 1
            }));
        }

        const msgPayload = {};

        if (poll) {
            msgPayload.poll = {
                name: poll.question,
                values: poll.options,
                selectableCount: 1
            };
        } else if (imageUrl) {
            msgPayload.image = { url: imageUrl };
            msgPayload.caption = message; // Caption is the text
        } else {
            msgPayload.text = message;
        }

        if (msgButtons.length > 0) {
            msgPayload.buttons = msgButtons;
            msgPayload.footer = "Food Delivery App"; // Required for buttons
            msgPayload.headerType = 1; // 1 = text, 4 = image? Let's try standard.
            // Actually for image + buttons, Baileys handles it if we pass 'buttons' prop with 'image'.
        }

        await sock.sendMessage(jid, msgPayload);

        console.log(`Sent message with ${msgButtons.length} buttons to ${formattedPhone}`);
        res.json({ success: true });
    } catch (error) {
        console.error('Error sending message:', error);
        res.status(500).json({ success: false, error: 'Failed to send message' });
    }
});

// Health Check
app.get('/health', (req, res) => {
    res.json({ status: 'UP', whatsapp_connected: !!sock });
});

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`WhatsApp Service running on port ${PORT}`);
});
