"""Generate the shared Roboto weights from the Android reference font.

Optional maintenance tool: requires fonttools==4.61.1, not needed for app builds.
Use --source <Android Studio>/plugins/design-tools/resources/layoutlib/data/fonts/Roboto-Regular.ttf.
"""
from pathlib import Path
import argparse
import hashlib
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

SOURCE_SHA256 = 'd5633ff957d2ecb1ce7716e86e11057091c3526042efe16fe1f65dfc06a65551'
WEIGHTS = {'regular': 400, 'medium': 500, 'semibold': 600, 'bold': 700, 'extrabold': 800}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, required=True)
    args = parser.parse_args()
    assert hashlib.sha256(args.source.read_bytes()).hexdigest() == SOURCE_SHA256, 'Unexpected reference font. Review version/license before updating.'
    root = Path(__file__).resolve().parent.parent
    target = root / 'designSystem/src/commonMain/composeResources/font'
    target.mkdir(parents=True, exist_ok=True)
    for style, weight in WEIGHTS.items():
        font = TTFont(args.source, recalcTimestamp=False)
        axes = {a.axisTag: a.defaultValue for a in font['fvar'].axes}
        axes.update(wght=weight, wdth=100, ital=0)
        instance = instantiateVariableFont(font, axes, inplace=False, updateFontNames=True)
        instance.recalcTimestamp = False
        path = target / f'roboto_{style}.ttf'
        instance.save(path)
        check = TTFont(path)
        assert 'fvar' not in check
        assert check['OS/2'].usWeightClass == weight
        assert all(ord(c) in check.getBestCmap() for c in 'çãáàâéêíóôõúüÇÃÁÀÂÉÊÍÓÔÕÚÜ')
        print(path.name, path.stat().st_size, hashlib.sha256(path.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
