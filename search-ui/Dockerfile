FROM node:latest

LABEL org.opencontainers.image.source="https://github.com/dyang912/NewsSearchEngine"

WORKDIR /app


# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH

COPY package.json ./
COPY package-lock.json ./
RUN export NODE_OPTIONS=--openssl-legacy-provider
RUN npm install --production
COPY . ./

CMD ["npm", "start"]

