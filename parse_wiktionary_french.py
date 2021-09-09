import sys
import time
import sqlite3
from xml.etree import ElementTree as ET

from wikitextparser import remove_markup, parse

PARTS_OF_SPEECH = {
    "nom",
    "adjectif",
    "verbe",
    "pronom",
    "adverbe",
    "préposition",
    "conjunction",
    "interjection",
}

"""
Table schema :

CREATE TABLE dictionary
                             (
                                 id INTEGER PRIMARY KEY,
                                 word TEXT,
                                 lexical_category TEXT,
                                 etymology_no INTEGER,
                                 definition_no INTEGER,
                                 definition TEXT
                             );

Downloading latest dump file :

wget https://dumps.wikimedia.org/frwiktionary/latest/frwiktionary-latest-pages-articles.xml.bz2
"""


def main():
    connection = sqlite3.connect("database_fr.db")
    cursor = connection.cursor()
    cursor.execute("DELETE from dictionary")
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
                for section in parse(content).sections:
                    templates = section.templates
                    if templates:
                        # Get argument 1 which is part of speech and check for value
                        template_arguments = templates[0].arguments
                        part_of_speech = template_arguments[0].value

                        # Skip sections not french since some translations are also present
                        if (
                            len(template_arguments) > 1
                            and template_arguments[1].value != "fr"
                        ):
                            continue

                        # Validate parts of speech. There is also nom proper so just check if nom is there
                        if any(
                            part in PARTS_OF_SPEECH
                            for part in part_of_speech.lower().split()
                        ):
                            meanings = [
                                remove_markup(item).strip()
                                for item in section.lists()[0].items
                            ]  # Get list of meanings and remove markup for display
                            for meaning in meanings:
                                index += 1
                                cursor.execute(
                                    "INSERT INTO dictionary VALUES (?, ?, ?, ?, ?, ?)",
                                    (index, title, part_of_speech, 1, 1, meaning),
                                )
            except (Exception, IndexError) as e:
                elem.clear()
                continue

            # https://stackoverflow.com/questions/12160418/why-is-lxml-etree-iterparse-eating-up-all-my-memory
            elem.clear()
            words += 1
            count += 1

            if count > 1000:
                count = 0
                cursor.execute("COMMIT")
                connection.commit()
                cursor.execute("BEGIN TRANSACTION")
                print(f"Tag count {tag_count}")
                print(
                    f"Processing {words} words and {index} meanings took {time.time()-start_time} seconds"
                )

    cursor.close()
    connection.close()
    print(f"Processing {words} words took {time.time()-start_time} seconds")


if __name__ == "__main__":
    main()
