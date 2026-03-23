-- Pedidos de exemplo (simulando pedidos de um app tipo iFood)
INSERT INTO pedidos (id, data_hora, status) VALUES
(1, '2026-03-20 12:30:00', 'REALIZADO'),
(2, '2026-03-20 13:15:00', 'PAGO'),
(3, '2026-03-20 14:00:00', 'CONFIRMADO'),
(4, '2026-03-19 19:45:00', 'ENTREGUE'),
(5, '2026-03-19 20:30:00', 'ENTREGUE');

-- Itens dos pedidos (itens de restaurante)
INSERT INTO item_do_pedido (id, descricao, quantidade, pedido_id) VALUES
-- Pedido 1: Hambúrgueria
(1, 'X-Bacon Artesanal', 2, 1),
(2, 'Batata Frita Grande', 1, 1),
(3, 'Refrigerante 600ml', 2, 1),

-- Pedido 2: Pizzaria
(4, 'Pizza Calabresa Grande', 1, 2),
(5, 'Pizza Margherita Média', 1, 2),
(6, 'Guaraná 2L', 1, 2),

-- Pedido 3: Japonês
(7, 'Combo Sushi 30 peças', 1, 3),
(8, 'Temaki Salmão', 2, 3),
(9, 'Gyoza (8 unidades)', 1, 3),
(10, 'Chá Gelado', 2, 3),

-- Pedido 4: Açaí
(11, 'Açaí 500ml com Granola', 1, 4),
(12, 'Açaí 300ml com Frutas', 1, 4),

-- Pedido 5: Marmita
(13, 'Marmita Executiva - Frango Grelhado', 1, 5),
(14, 'Marmita Executiva - Picanha', 1, 5),
(15, 'Suco Natural 500ml', 2, 5);
