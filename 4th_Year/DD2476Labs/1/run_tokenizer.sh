#!/usr/bin/bash
java -cp classes ir.TokenTest -f token_test.txt -p patterns.txt -rp -cf > tokenized_result.txt
