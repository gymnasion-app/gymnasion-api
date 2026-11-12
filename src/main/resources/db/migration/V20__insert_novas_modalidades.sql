-- V20__insert_novas_modalidades.sql
-- Adiciona novas modalidades desportivas ao catalogo oficial da plataforma, incluindo Wrestling

INSERT INTO modalidade (nome, descricao, ativo)
SELECT novos.nome, novos.descricao, TRUE
FROM (
    VALUES
        ('Wrestling', 'Esporte de combate corpo a corpo focado em quedas, projeções, imobilizações e controle posicional.'),
        ('Futebol', 'Treinamento esportivo focado em resistência cardiovascular, agilidade motora, velocidade e condução de bola.'),
        ('Futsal', 'Futebol de salão de alta intensidade, demandando tomadas de decisão rápidas, reflexos e explosão muscular.'),
        ('Handebol', 'Esporte coletivo com foco em velocidade de deslocamento, potência de arremesso e resistência cardiorrespiratória.'),
        ('Karatê', 'Arte marcial focada em disciplina, controle corporal, precisão de socos e chutes e reflexos rápidos.'),
        ('Taekwondo', 'Arte marcial coreana com forte ênfase em chutes dinâmicos, agilidade, flexibilidade e potência.'),
        ('Krav Maga', 'Sistema de combate e defesa pessoal voltado para reflexos rápidos, força funcional e condicionamento físico geral.'),
        ('Kickboxing', 'Arte marcial e esporte de combate combinando técnicas de socos e chutes de alta intensidade e gasto calórico.'),
        ('Rugby', 'Esporte coletivo de alta intensidade física e impacto, exigindo velocidade, explosão muscular e resistência.'),
        ('Remo', 'Treinamento aquático focado em força de tração, capacidade cardiorrespiratória e resistência muscular de membros e tronco.'),
        ('Canoagem', 'Modalidade aquática focada em potência de membros superiores, rotação e estabilização de core e equilíbrio.'),
        ('Surf', 'Esporte aquático focado no desenvolvimento de capacidade de remada, flexibilidade, equilíbrio na prancha e mobilidade.'),
        ('Skate', 'Prática esportiva focada em equilíbrio dinâmico, coordenação motora, agilidade e fortalecimento articular de membros inferiores.'),
        ('Triatlo', 'Treinamento combinado e contínuo de natação, ciclismo e corrida para resistência e condicionamento extremo.'),
        ('Escalada', 'Atividade física e esportiva de alta demanda em força isométrica, resistência de pegada, flexibilidade e equilíbrio.'),
        ('Powerlifting', 'Treinamento de força máxima baseado nos levantamentos básicos: agachamento, supino reto e levantamento terra.'),
        ('Badminton', 'Esporte de raquete focado em agilidade extrema, reflexos instantâneos, velocidade multidirecional e impulsão.'),
        ('Tênis de Mesa', 'Esporte de raquete de alta velocidade com foco em coordenação visomotora fina, reflexos imediatos e agilidade.'),
        ('Padel', 'Modalidade de raquete em duplas praticada em quadra fechada com ênfase em estratégia rápida, agilidade e condicionamento aeróbico.'),
        ('Atletismo', 'Conjunto de provas de pista e campo focado em velocidade pura, impulsão, resistência e potência motora.')
) AS novos(nome, descricao)
WHERE NOT EXISTS (
    SELECT 1 FROM modalidade m WHERE LOWER(m.nome) = LOWER(novos.nome)
);
