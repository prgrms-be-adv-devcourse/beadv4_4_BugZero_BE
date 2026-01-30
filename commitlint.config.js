module.exports = {
    extends: ['@commitlint/config-conventional'],

    // 자동 생성 커밋 예외
    ignores: [
        (msg) =>
            msg.startsWith('Merge ') ||
            msg.startsWith('Revert ') ||
            msg.startsWith('fixup! ') ||
            msg.startsWith('squash! '),
    ],

    rules: {
        // <type>
        'type-enum': [
            2,
            'always',
            ['feat', 'fix', 'docs', 'style', 'refactor', 'test', 'chore'],
        ],

        // <scope>
        'scope-empty': [2, 'never'],

        // <subject>
        'subject-empty': [2, 'never'],
        'subject-full-stop': [2, 'never', '.'],
        'subject-max-length': [2, 'always', 72],
        'subject-case': [0], // 한글 허용
    },
};