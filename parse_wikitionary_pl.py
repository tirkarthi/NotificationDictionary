import sys
import re
import time
import sqlite3
from zipfile import ZipFile, ZIP_DEFLATED

from xml.etree import ElementTree as ET

from wikitextparser import remove_markup, parse

"""
Downloading latest dump file :

wget https://dumps.wikimedia.org/plwiktionary/latest/plwiktionary-latest-pages-articles.xml.bz2

Parsing structure

znaczenia (meaning start marker)

rzeczownik (part of speech)

    (1.1) poet. wieczór
    (1.2) vespers: rel. nieszpory
    (1.3) rel. dzwon wzywający na nieszpory

odmiana (meaning end marker)
"""


SQL_CREATE_TABLE = """
CREATE TABLE IF NOT EXISTS dictionary
                             (
                                 id INTEGER PRIMARY KEY,
                                 word TEXT,
                                 lexical_category TEXT,
                                 etymology_no INTEGER,
                                 definition_no INTEGER,
                                 definition TEXT
                             );
"""

SQL_DELETE_ENTRIES = "DELETE from dictionary"
DATABASE_FILE = "dictionary_pl.db"


def main():
    connection = sqlite3.connect(DATABASE_FILE)
    cursor = connection.cursor()
    cursor.execute(SQL_CREATE_TABLE)
    cursor.execute(SQL_DELETE_ENTRIES)
    start_time = time.time()
    doc = ET.iterparse(sys.argv[1])
    index = 0
    words = 0
    count = 0

    for event, elem in doc:

        if "page" in elem.tag:
            title = elem.find(
                ".//{http://www.mediawiki.org/xml/export-0.10/}title"
            ).text
            content = (
                elem.find(".//{http://www.mediawiki.org/xml/export-0.10/}revision")
                .find(".//{http://www.mediawiki.org/xml/export-0.10/}text")
                .text
            )

            try:
                sections = parse(content).sections
                for section in sections:
                    if section.templates:
                        start, end = None, None
                        for template in section.templates:
                            if template.name == "odmiana":
                                end = template

                            if template.name == "znaczenia":
                                start = template

                        if start and end:
                            content = parse(
                                section.string[
                                    start.span[0]
                                    - section.span[0] : end.span[1]
                                    - section.span[0]
                                ]
                            )
                            part_of_speech = content.get_italics()[0].text
                            meanings = content.lists()[0].items

                            for meaning_ in meanings:
                                index += 1
                                meaning = remove_markup(meaning_).strip()
                                meaning = re.sub(
                                    "^\s*\(\s*\d+\s*\.\s*\d+\s*\)", "", meaning
                                )
                                cursor.execute(
                                    "INSERT INTO dictionary VALUES (?, ?, ?, ?, ?, ?)",
                                    (index, title, part_of_speech, 1, 1, meaning),
                                )
                                print(index, title, part_of_speech, 1, 1, meaning)
            except (Exception, IndexError) as e:
                elem.clear()
                print(e)
                continue

            if count > 1000:
                count = 0
                cursor.execute("COMMIT")
                connection.commit()
                cursor.execute("BEGIN TRANSACTION")
                print(
                    f"Processing {words} words and {index} meanings took"
                    f" {time.time()-start_time} seconds"
                )

            # https://stackoverflow.com/questions/12160418/why-is-lxml-etree-iterparse-eating-up-all-my-memory
            elem.clear()
            words += 1
            count += 1

    cursor.close()
    connection.close()
    print(f"Processing {words} words took {time.time()-start_time} seconds")

    with ZipFile(f"{DATABASE_FILE}.zip", "w", ZIP_DEFLATED) as zipf:
        zipf.write(DATABASE_FILE)


if __name__ == "__main__":
    main()
