-- Pagamentos de exemplo (vinculados aos pedidos do serviço de pedidos)
INSERT INTO pagamentos (id, valor, nome, numero, expiracao, codigo, status, forma_de_pagamento_id, pedido_id) VALUES
(1, 89.90, 'João Silva', '4111111111111111', '12/2028', '123', 'CRIADO', 1, 1),
(2, 125.00, 'Maria Santos', '5500000000000004', '06/2027', '456', 'CONFIRMADO', 1, 2),
(3, 187.50, 'Carlos Oliveira', '340000000000009', '09/2029', '789', 'CONFIRMADO', 2, 3),
(4, 45.00, 'Ana Costa', NULL, NULL, NULL, 'CONFIRMADO', 3, 4),
(5, 78.00, 'Pedro Souza', NULL, NULL, NULL, 'CONFIRMADO', 3, 5);
