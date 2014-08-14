package com.wikipmodel

case class Document(docId: String = "", body: String = "", labels: Set[String] = Set.empty)
