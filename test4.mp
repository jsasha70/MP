func test:5:1 {
  def { a:1 b:1 c:1 d:1 e:1 }
  def { r1:1 r2:1 r3:1 r4:1 r5:1 r6:1 }

  >a:1 >b:1 >c:1 >d:1 >e:1.

  a:1> b:1> c:1> d:1> e:1> 0:1> >im:2:1> >im:2:1> >im:2:1> >im:2:1> >im:2:1> >r1:1.
  a:1> c:1> e:1> 0:1> >im:2:1> >im:2:1> >im:2:1> >r2:1.
  c:1> d:1> e:1> 0:1> >im:2:1> >im:2:1> >im:2:1> >r3:1.
  a:1> b:1> c:1> 0:1> >im:2:1> >im:2:1> >im:2:1> >r4:1.
  b:1> c:1> 0:1> >im:2:1> >im:2:1> >r5:1.
  d:1> c:1> 0:1> >im:2:1> >im:2:1> >r6:1.

  r1:1> r2:1> r3:1> r4:1> r5:1> r6:1> 0:1>
    >im:2:1> >im:2:1> >im:2:1> >im:2:1> >im:2:1> >im:2:1>
}
