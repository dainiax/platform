FROM anapsix/alpine-java

ARG build_env=dev
ARG config_bucket
ARG project

RUN apk update && apk add python3 && pip3 install awscli && rm -rf /var/cache/apk

ADD $project/target/universal/stage /opt/docker
RUN chown -R daemon:daemon /opt/docker
USER daemon

COPY run.sh /run.sh

EXPOSE 8888

ENV BUILD_ENV $build_env
ENV CONFIG_BUCKET $config_bucket
ENV PROJECT $project

CMD ["/run.sh"]