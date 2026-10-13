-- Ежедневный квест: физподготовка
INSERT INTO quests (type, title, description, targets, exp_reward, stat_reward, stat_reward_amount, reset_interval, ai_context, file_reference)
VALUES (
           'DAILY',
           'Ежедневная тренировка',
           'Выполни комплекс физических упражнений',
           '{"pushups": 20, "squats": 30, "plank": 60}',
           100,
           'strength',
           1,
           'DAILY',
           'Ежедневный квест на физическую подготовку. Нужно сделать 20 отжиманий, 30 приседаний и 60 секунд планки. При двойном выполнении активируется скрытый квест.',
           'quests/daily-workout.md'
       );

-- Скрытый ежедневный квест: двойная норма
INSERT INTO quests (type, title, description, targets, exp_reward, stat_reward, stat_reward_amount, reset_interval, ai_context, file_reference)
VALUES (
           'HIDDEN',
           'Скрытая тренировка',
           'Ты превзошёл свои возможности и сделал в два раза больше!',
           '{"pushups": 40, "squats": 60, "plank": 120}',
           200,
           'agility',
           1,
           'DAILY',
           'Скрытый ежедневный квест, активируется при двойном выполнении основного квеста. Награда — дополнительный опыт и ловкость.',
           'quests/hidden-double.md'
       );