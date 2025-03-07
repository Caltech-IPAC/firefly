FROM python:3.10

RUN apt-get update && \
	apt install -y vim && \
    pip install healpy && \
    pip install duckdb

WORKDIR /app

CMD ["bash"]

