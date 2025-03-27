cmake_minimum_required(VERSION 3.4.1)

add_library(graphics SHARED graphics.c)

target_link_libraries(graphics -ljnigraphics)