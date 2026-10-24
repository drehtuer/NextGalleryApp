# Build environment for NextGalleryApp: JDK 21 + Android SDK (API 37).
# Used locally as a devcontainer and in CI (devcontainers/ci), so both build the same way.
FROM eclipse-temurin:21-jdk-noble

ARG CMDLINE_TOOLS_VERSION=16111833
ARG CMDLINE_TOOLS_SHA1=e025545c62a8e64c7559119566a569fb1dec5f60
ARG ANDROID_PLATFORM=android-37.0
# Build-tools version that AGP uses by default (AGP 9.4: 36.0.0).
ARG BUILD_TOOLS_VERSION=36.0.0

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update \
    && apt-get install -y --no-install-recommends git unzip curl ca-certificates sudo \
    && rm -rf /var/lib/apt/lists/* \
    && echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/ubuntu

RUN curl -fsSL -o /tmp/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    && echo "${CMDLINE_TOOLS_SHA1}  /tmp/cmdline-tools.zip" | sha1sum -c - \
    && mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
    && mv /tmp/cmdline-tools/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip

# Accepting the Android SDK licenses is required to install SDK packages. The SDK stays writable for
# all users because the devcontainer remaps the build user's UID to the host UID at startup.
RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS_VERSION}" \
    && chmod -R a+rwX "${ANDROID_HOME}"

# Pre-create the Gradle home so the named volume mounted there is owned by the build user.
RUN mkdir -p /home/ubuntu/.gradle && chown -R ubuntu:ubuntu /home/ubuntu/.gradle

USER ubuntu
