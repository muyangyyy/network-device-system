FROM node:22-alpine AS build
# 注意必须 node>=22：lockfile 里的 @vueuse/core@15 要求 node>=22（EBADENGINE 只是警告，
# 但 node18 下 vue-tsc 会多出 28 个类型错误——element-plus 的类型链依赖新引擎行为）
WORKDIR /app
# 必须连 lockfile 一起拷并用 npm ci：裸 npm install 会解析到更新版的 element-plus，
# 其更严格的表格/开关类型定义会让 vue-tsc 直接报错（本地验证通过的版本组合被打破）
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
