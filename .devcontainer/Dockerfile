FROM mcr.microsoft.com/devcontainers/java:1-21

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
      build-essential \
      autoconf \
      automake \
      texinfo \
      libtool \
      libltdl-dev \
      libx11-dev \
      zsh \
      curl \
      wget \
    && echo "Image ready."
