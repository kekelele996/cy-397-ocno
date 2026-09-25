CREATE TABLE IF NOT EXISTS contract_templates (id BIGINT PRIMARY KEY AUTO_INCREMENT, type VARCHAR(32), title VARCHAR(120), content TEXT, variables JSON);
CREATE TABLE IF NOT EXISTS contracts (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, template_id BIGINT, title VARCHAR(120), content MEDIUMTEXT, status VARCHAR(32), signed_at DATETIME, signers JSON);
CREATE TABLE IF NOT EXISTS sign_invitations (id BIGINT PRIMARY KEY AUTO_INCREMENT, contract_id BIGINT, deadline DATETIME, status VARCHAR(32), created_at DATETIME);
CREATE TABLE IF NOT EXISTS sign_records (id BIGINT PRIMARY KEY AUTO_INCREMENT, invitation_id BIGINT, contract_id BIGINT, signer VARCHAR(120), status VARCHAR(32), signed_at DATETIME);
CREATE TABLE IF NOT EXISTS legal_tickets (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT, type VARCHAR(32), description TEXT, status VARCHAR(32), attachments JSON);
CREATE TABLE IF NOT EXISTS legal_faq (id BIGINT PRIMARY KEY AUTO_INCREMENT, category VARCHAR(60), question VARCHAR(200), answer TEXT);
INSERT INTO legal_faq(category, question, answer) VALUES ('合同纠纷','合同逾期未签署怎么办','可先发出书面催告并保存沟通证据。') ON DUPLICATE KEY UPDATE question=question;
