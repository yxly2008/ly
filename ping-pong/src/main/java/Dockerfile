##下载centos7镜像
FROM centos:centos7.4.1708

##作者
MAINTAINER ly

##安装jdk
RUN mkdir /usr/soft
RUN mkdir /usr/local/jdk
COPY jdk-8u60-linux-x64.tar.gz /usr/soft/jdk-8u60-linux-x64.tar.gz
RUN tar xvf /usr/soft/jdk-8u60-linux-x64.tar.gz -C /usr/local/jdk
RUN rm -rf /usr/soft/jdk-8u60-linux-x64.tar.gz

##设置环境变量
ENV JAVA_HOME /usr/local/jdk/jdk1.8.0_60
ENV PATH $PATH:$JAVA_HOME/bin:$CATCHA_HOME/bin

##内部开启服务端口
EXPOSE 8080