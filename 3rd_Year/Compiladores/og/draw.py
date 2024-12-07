# first you need to
# sudo apt install graphviz
# wget https://github.com/LuisaSanto/xpl-compiler/raw/master/xml2dot.jar

import sys
import os

def print_file(file_name):
	f = open(file_name)
	s = f.readline()
	while s:
		print(s, end="")
		s = f.readline()
	f.close()


def draw_tree(test_name):
	os.system("./og --target xml < "+test_name+" > out.xml")
	os.system("java -cp xml2dot.jar xml2dot.xml2dot out.xml ")
	os.system("kill `ps x | grep /usr/bin/lefty | awk '{print $1;}'`")
	os.system("dot -Tpng out.xml.dot -o out-imgs/"+test_name.split("/")[-1]+".png")


if(len(sys.argv) != 2 or not ((".og" in sys.argv[1]) or ("all" == sys.argv[1])) ):
	print("usage: python3 draw.py <file>.og")
	print("     : python3 draw.py all")
	print("the output images will be under /out-imgs")
	print("you must be at the project root")
	print("if you want to draw all tests, you must have the auto-tests dir inside the root dir")
else:
	if(not os.path.exists("out-imgs")):
		os.system("mkdir out-imgs")
	if(sys.argv[1] == "all"):
		for test in os.listdir("auto-tests"):
			if(".og" in test):
				draw_tree("auto-tests/"+test)
	else:
		draw_tree(sys.argv[1])
