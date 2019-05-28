#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  echo "dumping log: " 1>&2
  echo
  cat .travis/post.txt
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

