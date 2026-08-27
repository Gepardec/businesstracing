import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

export default {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter(),
    alias: {
      $components: 'src/lib/components',
      $contracts: 'src/lib/contracts',
      $graph: 'src/lib/graph',
      $runs: 'src/lib/runs'
    },
    csp: {
      mode: 'auto',
      directives: {
        'default-src': ['self'],
        'script-src': ['self'],
        'style-src': ['self', 'unsafe-inline'],
        'img-src': ['self', 'data:'],
        'connect-src': ['self'],
        'worker-src': ['self'],
        'object-src': ['none'],
        'base-uri': ['none'],
        'frame-ancestors': ['none']
      }
    }
  }
};
