filename = treepaper

pdf:
	pdflatex ${filename}

bib:
	bibtex ${filename}

all: 
	pdflatex ${filename}
	bibtex ${filename}
	pdflatex ${filename}
	pdflatex ${filename}

clean:
	rm -f *.log *.aux *.bbl *.blg *~
	rm -f ${filename}.pdf
