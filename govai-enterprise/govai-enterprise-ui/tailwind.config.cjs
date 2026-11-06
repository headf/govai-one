/** GovAI One 设计 Token（Tailwind v3）
 *  - 与 Element Plus 共存：关闭 preflight，避免重置 Element 基础样式
 *  - 颜色/圆角/阴影/字号全部来自《GovAI One 统一视觉规范》
 */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  corePlugins: { preflight: false },
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#2F6BFF', deep: '#1A3A8F', dark: '#2656CC', 50: '#EAF0FF' },
        canvas: '#F5F6F8',
        surface: { DEFAULT: '#FFFFFF', muted: '#F7F8FA' },
        ink: { DEFAULT: '#1F2937', 2: '#606266', 3: '#9CA3AF' },
        line: '#E5E7EB',
        'line-soft': 'rgba(16, 24, 40, 0.14)',
        'line-brand': 'rgba(47, 107, 255, 0.30)',
      },
      borderRadius: { sm: '6px', md: '12px', lg: '16px' },
      boxShadow: {
        card: '0 1px 2px rgba(16,24,40,.04), 0 1px 3px rgba(16,24,40,.06)',
        'card-hover': '0 2px 4px rgba(16,24,40,.06), 0 8px 20px rgba(47,107,255,.10)',
      },
      fontSize: {
        caption: ['12px', { lineHeight: '1.5' }],
        body: ['14px', { lineHeight: '1.7' }],
        subtitle: ['16px', { lineHeight: '1.5', fontWeight: '600' }],
        title: ['20px', { lineHeight: '1.4', fontWeight: '600' }],
        display: ['28px', { lineHeight: '1.3', fontWeight: '700' }],
      },
    },
  },
  plugins: [],
}
