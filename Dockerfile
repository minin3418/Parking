FROM node:20

# Создаем пользователя user (id 1000) для Hugging Face
RUN useradd -m -u 1000 user
USER user
ENV HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH

WORKDIR $HOME/app

# Копируем с правами пользователя, чтобы база данных могла сохраняться
COPY --chown=user . $HOME/app
RUN npm install

EXPOSE 7860
ENV PORT=7860
CMD ["node", "server.js"]
