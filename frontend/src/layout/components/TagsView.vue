<template>
  <div class="tags-view-container" v-if="tags.length > 0">
    <el-scrollbar>
      <div class="tags-view-wrapper">
        <router-link
          v-for="tag in tags"
          :key="tag.path"
          :to="tag.path"
          class="tags-view-item"
          :class="{ active: isActive(tag) }"
        >
          {{ tag.title }}
          <el-icon v-if="!tag.affix" class="tag-close" @click.prevent.stop="closeTag(tag)">
            <Close />
          </el-icon>
        </router-link>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'

interface TagView {
  path: string
  title: string
  affix?: boolean
}

const route = useRoute()
const router = useRouter()

const tags = computed<TagView[]>(() => {
  // route.matched 里记录的 path 是路由定义模板（如 '/devices/edit/:id'），
  // 而不是实际路径（'/devices/edit/1'）。旧写法直接用模板路径会导致：
  // ① 点击标签跳转字面 '/devices/edit/:id' → 落 catch-all 回仪表盘；
  // ② isActive 比较永远不等 → 标签不高亮。
  // 因此跳转地址必须用 route.path（当前实际路径）。
  // 本项目路由为单层结构，matched 链上有 title 的只有当前路由一条；
  // hidden 页面（编辑/详情等）同样显示标签，为其提供「关闭返回仪表盘」的出口。
  const leaf = route.matched[route.matched.length - 1]
  if (!leaf?.meta?.title) return []
  return [
    {
      path: route.path,
      title: leaf.meta.title as string,
      affix: leaf.meta.affix as boolean
    }
  ]
})

function isActive(tag: TagView) {
  return tag.path === route.path
}

function closeTag(tag: TagView) {
  const index = tags.value.findIndex((t) => t.path === tag.path)
  if (index > -1) {
    const remaining = tags.value.filter((t) => t.path !== tag.path)
    if (remaining.length > 0 && isActive(tag)) {
      router.push(remaining[Math.min(index, remaining.length - 1)].path)
    } else if (remaining.length === 0) {
      router.push('/dashboard')
    }
  }
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: $tagsview-height;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.tags-view-wrapper {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 100%;
  gap: 4px;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  color: #666;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  text-decoration: none;
  white-space: nowrap;
  gap: 4px;
  transition: all 0.2s;

  &:hover {
    color: $primary-color;
  }

  &.active {
    color: #fff;
    background-color: $primary-color;
    border-color: $primary-color;
  }

  .tag-close {
    font-size: 12px;
    border-radius: 50%;
    cursor: pointer;
    &:hover {
      background-color: rgba(0, 0, 0, 0.1);
    }
  }
}
</style>
