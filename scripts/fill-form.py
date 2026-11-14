"""docs/PROPOSAL.md 의 `## N.` 섹션을 공식 접수양식(DOCX) 02 섹션 N번 박스에 채운다.

사용: python3 -I scripts/fill-form.py <원본.docx> <PROPOSAL.md> <출력.docx>
개인정보 칸(01 참가자 정보·04 서명)은 건드리지 않고, 앱명 줄만 'Blow' 로 채운다.
"""
import re
import sys
import zipfile
from xml.sax.saxutils import escape

src, md_path, out = sys.argv[1:4]
APP_NAME = 'Blow'

md = open(md_path, encoding='utf-8').read()
sections = {}
for m in re.finditer(r'^## (\d)\. [^\n]*\n(.*?)(?=^## \d\. |\Z)', md, re.S | re.M):
    sections[int(m.group(1))] = m.group(2).strip('\n')
assert sorted(sections) == list(range(1, 9)), sections.keys()

PPR = '<w:pPr><w:spacing w:after="40" w:line="300" w:lineRule="auto"/>{ind}</w:pPr>'
RPR = '<w:rPr><w:rFonts w:hint="eastAsia"/>{b}<w:sz w:val="20"/><w:szCs w:val="20"/><w:lang w:eastAsia="ko-KR"/></w:rPr>'


def run(text, bold=False):
    return f'<w:r>{RPR.format(b="<w:b/><w:bCs/>" if bold else "")}<w:t xml:space="preserve">{escape(text)}</w:t></w:r>'


def para(line):
    ind = ''
    if not line.strip():
        return f'<w:p>{PPR.format(ind="")}</w:p>'
    if line.startswith('  - '):
        line, ind = '◦ ' + line[4:], '<w:ind w:left="560" w:hanging="200"/>'
    elif line.startswith('- '):
        line, ind = '• ' + line[2:], '<w:ind w:left="280" w:hanging="200"/>'
    runs = []
    # **굵게** 구간 처리
    for i, part in enumerate(re.split(r'\*\*', line)):
        if part:
            runs.append(run(part, bold=i % 2 == 1))
    return f'<w:p>{PPR.format(ind=ind)}{"".join(runs)}</w:p>'


with zipfile.ZipFile(src) as z:
    files = {n: z.read(n) for n in z.namelist()}
xml = files['word/document.xml'].decode('utf-8')

# 02 섹션 박스 = 문서의 2~9번째 표 (1번째는 참가 구성원 표)
tbl_spans = []
pos = 0
while True:
    i = xml.find('<w:tbl>', pos)
    if i < 0:
        break
    j = xml.index('</w:tbl>', i) + len('</w:tbl>')
    tbl_spans.append((i, j))
    pos = j
assert len(tbl_spans) == 10, len(tbl_spans)

# 뒤에서부터 바꿔야 앞쪽 오프셋이 유지된다
for n in range(8, 0, -1):
    i, j = tbl_spans[n]
    tbl = xml[i:j]
    tc_open = tbl.index('</w:tcPr>') + len('</w:tcPr>')
    tc_close = tbl.rindex('</w:tc>')
    body = ''.join(para(l) for l in sections[n].split('\n'))
    xml = xml[:i] + tbl[:tc_open] + body + tbl[tc_close:] + xml[j:]

# 앱명 줄 채우기 (01·02·04 세 곳) — 밑줄이 여러 run 으로 쪼개져 있어 뒤따르는 밑줄 run 까지 지운다
xml, k = re.subn(
    r'(프로젝트명: )_+(</w:t></w:r>)((?:<w:r[^>]*>(?:<w:rPr>(?:(?!</w:r>).)*?</w:rPr>)?<w:t[^>]*>_+</w:t></w:r>)*)',
    r'\g<1>' + APP_NAME + r'\g<2>', xml)
assert k == 3, k

files['word/document.xml'] = xml.encode('utf-8')
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for n, data in files.items():
        z.writestr(n, data)
print('ok', out)
