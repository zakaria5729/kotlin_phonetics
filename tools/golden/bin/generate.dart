import 'dart:io';

import 'package:flutter_phonetics/flutter_phonetics.dart';

const banglaWords = [
  'মাসুদ', 'আসাদ', 'রহিম', 'করিম', 'সুমন', 'শুভ', 'সোহেল', 'ঢাকা', 'চট্টগ্রাম', 'বাংলাদেশ',
  'কলকাতা', 'রাজশাহী', 'খুলনা', 'বরিশাল', 'সিলেট', 'রংপুর', 'ময়মনসিংহ', 'কুমিল্লা', 'গাজীপুর', 'নারায়ণগঞ্জ',
  'আনন্দ', 'ভালোবাসা', 'বন্ধু', 'মানুষ', 'পৃথিবী', 'সূর্য', 'চাঁদ', 'আকাশ', 'নদী', 'পাহাড়',
  'শহর', 'গ্রাম', 'স্কুল', 'বিদ্যালয়', 'শিক্ষক', 'ছাত্র', 'ছাত্রী', 'বই', 'কলম', 'খাতা',
  'ভাত', 'মাছ', 'মাংস', 'ডাল', 'রুটি', 'চা', 'পানি', 'দুধ', 'ফল', 'সবজি',
  'জ্ঞান', 'বিজ্ঞান', 'যন্ত্র', 'কম্পিউটার', 'ফোন', 'ইন্টারনেট', 'গান', 'নাচ', 'খেলা', 'ছবি',
  'ঋতু', 'বর্ষা', 'শরৎ', 'হেমন্ত', 'শীত', 'বসন্ত', 'গ্রীষ্ম', 'ঝড়', 'বৃষ্টি', 'রোদ',
  'আমি', 'তুমি', 'সে', 'আমরা', 'তোমরা', 'তারা', 'এই', 'ওই', 'কেন', 'কোথায়',
  'ঈদ', 'পূজা', 'উৎসব', 'বিয়ে', 'জন্ম', 'মৃত্যু', 'স্বপ্ন', 'আশা', 'ভয়', 'সাহস',
  'ঐশ্বর্য', 'ঔষধ', 'ঋণ', 'ঈশ্বর', 'উপহার', 'ঊষা', 'এক', 'দুই', 'তিন', 'চার',
  'পাঁচ', 'ছয়', 'সাত', 'আট', 'নয়', 'দশ', 'শত', 'সহস্র', 'লক্ষ', 'কোটি',
  'রহমান', 'হোসেন', 'আহমেদ', 'ইসলাম', 'খান', 'চৌধুরী', 'সরকার', 'দাস', 'ঘোষ', 'বসু',
  'ডাক্তার', 'ইঞ্জিনিয়ার', 'ব্যবসা', 'চাকরি', 'টাকা', 'দোকান', 'বাজার', 'রাস্তা', 'গাড়ি', 'ট্রেন',
  'বড় ভাই', 'ছোট বোন', 'আমার সোনার বাংলা', 'শুভ সকাল', 'ধন্যবাদ', 'মা-বাবা', 'নতুন বছর', 'ভালো থাকো',
  'ড়াকা', 'ঢ়াল', 'য়মুনা', 'পড়া', 'বড়', 'ঘোড়া', 'মেয়ে', 'দেয়াল',
  'কেউ', 'নৌকা', 'মৌসুমী', 'ঐতিহ্য', 'ভৈরব', 'সৈকত', 'বৈশাখ', 'কৌশল', 'পৌষ', 'গৌরব',
];

const banglaQueries = [
  'মাসুদ', 'রহিম', 'শহর', 'স্কুল', 'বাংলাদেশ', 'ঢাকা', 'জ্ঞান', 'ঋতু', 'ঐতিহ্য', 'ছাত্র',
  'পড়া', 'আমার সোনার বাংলা', 'ক', '', 'abc', 'শুভ সকাল',
];

const englishWords = [
  'Mary Ann', 'Mary', 'Ann', 'Jose', 'Wright', 'Catherine', 'Katherine', 'Kathryn', 'Stephen', 'Steven',
  'Philip', 'Phillip', 'Sean', 'Shaun', 'Shawn', 'Jon', 'John', 'Jonathan', 'Johnathan', 'Geoffrey',
  'Jeffrey', 'Zachary', 'Zackary', 'Zakaria', 'Zakariya', 'Zachariah', 'Mohammed', 'Muhammad', 'Mohammad', 'Mohamed',
  'Ahmed', 'Ahmad', 'Hossain', 'Hussain', 'Hussein', 'Hosen', 'Rahman', 'Rehman', 'Khan', 'Kahn',
  'Smith', 'Smyth', 'Schmidt', 'Schmitt', 'Taylor', 'Tailor', 'Brown', 'Browne', 'Green', 'Greene',
  'Clark', 'Clarke', 'Allan', 'Allen', 'Alan', 'Neil', 'Neal', 'Neill', 'Eric', 'Erik',
  'Isabel', 'Isabelle', 'Isobel', 'Elizabeth', 'Elisabeth', 'Sara', 'Sarah', 'Anna', 'Hannah', 'Hanna',
  'Michael', 'Mikael', 'Micheal', 'Nicholas', 'Nicolas', 'Nikolas', 'Lucy', 'Lucie', 'Lewis', 'Louis',
  'Knight', 'Night', 'Write', 'Right', 'Rite', 'Wrong', 'Rong', 'Gnome', 'Nome', 'Phone',
  'Fone', 'Whale', 'Wail', 'Which', 'Witch', 'Queen', 'Kween', 'Quick', 'Kwik', 'Exit',
  'Eksit', 'Taxi', 'Taksi', 'Coffee', 'Kofi', 'Cafe', 'Café', 'Naïve', 'Naive', 'Façade',
  'Facade', 'Müller', 'Mueller', 'Muller', 'José', 'Zoë', 'Zoe', 'Straße', 'Strasse', 'Øre',
  'Æsop', 'Łukasz', 'Lukasz', 'Dvořák', 'Dvorak', "O'Brien", "O’Brien", 'OBrien', 'Mc Donald', 'McDonald',
  'MACDONALD', 'macdonald', 'MacDonald', 'JOHN SMITH', 'john smith', 'John Smith', 'Anne-Marie', 'Annemarie', 'Jean-Luc', 'Mary  Ann',
  'sea', 'see', 'bee', 'be', 'bye', 'buy', 'by', 'two', 'too', 'to',
  'hear', 'here', 'there', 'their', 'they are', 'cat', 'kat', 'cut', 'cot', 'caught',
  'beautiful', 'butiful', 'necessary', 'neccessary', 'definitely', 'definately', 'separate', 'seperate', 'receive', 'recieve',
  'x', 'q', 'ph', 'sh', '', ' ', 'a', 'A', 'zz', 'ZZ',
];

const englishQueries = [
  'Mary', 'Smith', 'Katherine', 'Michael', 'Zakaria', 'Knight', 'Phone', 'Quick', 'Exit', 'Café',
  'Müller', 'O\'Brien', 'JOHN SMITH', 'Anne-Marie', 'a', '', 'বাংলা', 'sea',
];

const normalizeSamples = [
  [0x20, 0x41, 0x20],
  [0x09, 0x61, 0x0A],
  [0x61, 0x20, 0x20, 0x62],
  [0x61, 0xA0, 0x62],
  [0x61, 0x2003, 0x2003, 0x62],
  [0xFEFF, 0x61, 0xFEFF],
  [0x85, 0x61, 0x85],
  [0x61, 0x85, 0x62],
  [0x61, 0x01, 0x62],
  [0x1680, 0x61, 0x3000, 0x62],
  [0x41, 0x42, 0x43],
  [0xC0, 0xC9, 0xD6],
  [0x130, 0x49],
  [0xDF],
  [0x1E9E],
  [0x3A3, 0x3A3],
  [0x61, 0x300, 0x62, 0x301],
  [0x2019, 0x2018],
  [0x9A1, 0x9BC],
  [0x9A2, 0x9BC],
  [0x9AF, 0x9BC],
  [0x9A4, 0x9CD, 0x200D],
  [0x9C7, 0x9BE],
  [0x9C7, 0x9D7],
  [0x9A1, 0x9BC, 0x9A1, 0x9BC],
  [0x9B8, 0x9CD, 0x9AC],
  [0xD83D, 0xDE00, 0x61],
  [0x61, 0xD83D],
  [0x10400, 0x10428],
];

final languages = {'bn': banglaLanguage, 'en': englishLanguage};
final engines = {
  'bn': PhoneticEngine(language: banglaLanguage),
  'en': PhoneticEngine(language: englishLanguage),
};

String matchText(PhoneticMatch match) => '${match.word}:${match.score}';

String matchesText(List<PhoneticMatch> matches) => matches.map(matchText).join('|');

String hex(String text) =>
    text.codeUnits.map((unit) => unit.toRadixString(16).padLeft(4, '0')).join(' ');

void main(List<String> args) {
  final lines = <String>[];

  const generateSettings = <String, ({double minScore, int? maxResults, bool includeOriginal, int? maxChanges})>{
    'a': (minScore: 70, maxResults: 50, includeOriginal: true, maxChanges: null),
    'b': (minScore: 50, maxResults: 20, includeOriginal: false, maxChanges: null),
    'c': (minScore: 60, maxResults: 30, includeOriginal: true, maxChanges: 1),
    'd': (minScore: 0, maxResults: 15, includeOriginal: true, maxChanges: 2),
  };

  for (final code in ['bn', 'en']) {
    final engine = engines[code]!;
    final words = code == 'bn' ? banglaWords : englishWords;
    final queries = code == 'bn' ? banglaQueries : englishQueries;

    for (final word in words) {
      for (final entry in generateSettings.entries) {
        final setting = entry.value;
        final result = engine.generate(
          word,
          minScore: setting.minScore,
          maxResults: setting.maxResults,
          includeOriginal: setting.includeOriginal,
          maxChanges: setting.maxChanges,
        );
        if (result.error != null) {
          stderr.writeln('unexpected error for $word: ${result.error}');
          exit(1);
        }
        lines.add('G\t$code\t${entry.key}\t$word\t${matchesText(result.matches)}');
      }
    }

    final pairWords = words.where((word) => word.trim().isNotEmpty).toList();
    for (var i = 0; i < pairWords.length; i += 1) {
      for (final offset in [1, 2, 7]) {
        final other = pairWords[(i + offset) % pairWords.length];
        final result = engine.similarity(pairWords[i], other);
        lines.add('S\t$code\t${pairWords[i]}\t$other\t${result.score}');
      }
      final same = engine.similarity(pairWords[i], pairWords[i]);
      lines.add('S\t$code\t${pairWords[i]}\t${pairWords[i]}\t${same.score}');
    }
    lines.add('S\t$code\t\tabc\t${engine.similarity('', 'abc').score}');
    lines.add('S\t$code\tabc\t\t${engine.similarity('abc', '').score}');

    final candidates = words.where((word) => word.isNotEmpty).toList();
    for (final query in queries) {
      for (final setting in <String, ({double minScore, int? limit})>{
        'a': (minScore: 0, limit: null),
        'b': (minScore: 60, limit: 10),
        'c': (minScore: 85, limit: null),
        'd': (minScore: 0, limit: 5),
      }.entries) {
        final result = engine.rank(query, candidates,
            minScore: setting.value.minScore, limit: setting.value.limit);
        lines.add(
            'R\t$code\t${setting.key}\t$query\t${matchesText(result.matches)}');
      }
    }

    for (final sample in normalizeSamples) {
      final text = String.fromCharCodes(sample);
      lines.add('N\t$code\t${hex(text)}\t${hex(languages[code]!.normalize(text))}');
    }
  }

  File(args.first).writeAsStringSync(kotlinSource(lines));
  stdout.writeln('wrote ${lines.length} lines');
}

String kotlinSource(List<String> lines) {
  const chunkBytes = 24000;
  final chunks = <List<String>>[[]];
  var size = 0;
  for (final line in lines) {
    final lineBytes = line.codeUnits.fold<int>(0, (sum, unit) => sum + (unit < 0x80 ? 1 : 3)) + 1;
    if (size + lineBytes > chunkBytes) {
      chunks.add([]);
      size = 0;
    }
    chunks.last.add(line);
    size += lineBytes;
  }
  final out = StringBuffer()
    ..writeln('package io.github.zakaria5729.phonetics')
    ..writeln()
    ..writeln('internal val goldenData: String get() = buildString {');
  for (var i = 0; i < chunks.length; i++) {
    out.writeln('    append(goldenPart$i)');
  }
  out
    ..writeln('}')
    ..writeln();
  for (var i = 0; i < chunks.length; i++) {
    out
      ..writeln('private val goldenPart$i = """')
      ..writeln(chunks[i].join('\n'))
      ..writeln('"""')
      ..writeln();
  }
  return out.toString();
}
