#!/bin/bash
sudo docker run -it --rm -v `pwd`:/app:rw -p 9000:9000 ingensi/play-framework
