<template>
  <div class="particle-layer" :style="{ '--particle-glow': glowColor }">
    <component :is="particlesComponent" :id="containerId" :options="options" />
  </div>
</template>

<script setup lang="ts">
import { computed, resolveComponent } from 'vue'
import type { ISourceOptions } from '@tsparticles/engine'

const props = withDefaults(
  defineProps<{
    count?: number
    color?: string | string[]
    lineColor?: string
    lineOpacity?: number
    lineWidth?: number
    speed?: number
    linkDistance?: number
    interactive?: boolean
    glowColor?: string
  }>(),
  {
    count: 70,
    color: () => ['#dbeafe', '#93c5fd', '#60a5fa'],
    lineColor: '#93c5fd',
    lineOpacity: 0.65,
    lineWidth: 1.4,
    speed: 1.4,
    linkDistance: 150,
    interactive: true,
    glowColor: 'rgba(96, 165, 250, 0.35)'
  }
)

const particlesComponent = resolveComponent('vue-particles')
const containerId = `tsparticles-${Math.random().toString(36).slice(2, 9)}`

const options = computed<ISourceOptions>(() => ({
  fullScreen: {
    enable: false
  },
  background: {
    color: {
      value: 'transparent'
    }
  },
  fpsLimit: 120,
  detectRetina: true,
  interactivity: {
    events: {
      onClick: {
        enable: false
      },
      onHover: {
        enable: props.interactive,
        mode: 'grab'
      }
    },
    modes: {
      grab: {
        distance: 180,
        links: {
          opacity: 0.5
        }
      }
    }
  },
  particles: {
    color: {
      value: props.color
    },
    links: {
      color: props.lineColor,
      distance: props.linkDistance,
      enable: true,
      opacity: props.lineOpacity,
      width: props.lineWidth
    },
    move: {
      direction: 'none',
      enable: true,
      outModes: 'bounce',
      speed: props.speed,
      straight: false
    },
    number: {
      density: {
        enable: true
      },
      value: props.count
    },
    opacity: {
      value: {
        min: 0.35,
        max: 0.9
      }
    },
    shape: {
      type: 'circle'
    },
    size: {
      value: {
        min: 1,
        max: 4
      }
    }
  }
}))
</script>

<style scoped>
.particle-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.particle-layer :deep(canvas) {
  filter: drop-shadow(0 0 8px var(--particle-glow));
}
</style>
