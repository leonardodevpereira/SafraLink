INSERT INTO produtos (nome, grao, mercado) VALUES
    ('Soja Interno',   'SOJA',   'INTERNO'),
    ('Soja Externo',   'SOJA',   'EXTERNO'),
    ('Milho Interno',  'MILHO',  'INTERNO'),
    ('Milho Externo',  'MILHO',  'EXTERNO'),
    ('Feijão Interno', 'FEIJAO', 'INTERNO');

INSERT INTO quantidades_disponiveis (produto_id, quantidade)
SELECT id, 0 FROM produtos;
