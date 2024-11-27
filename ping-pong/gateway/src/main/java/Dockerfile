##下载centos7镜像
FROM ping_pong_base:latest

##作者
MAINTAINER ly

RUN mkdir /entry
COPY ./application.yml /usr/local/application.yml
COPY gateway-1.0-SNAPSHOT.jar /usr/local/gateway-1.0-SNAPSHOT.jar
COPY entrypoint.sh /entry/

RUN chmod 777 /entry/entrypoint.sh

#工作目录
WORKDIR /usr/local

# 启动脚本

ENTRYPOINT ["bash","/entry/entrypoint.sh"]