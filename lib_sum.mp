func add1:3:2 { // c s add -> c s
  if { // add=1
    if { // s=1
      >_:1 10#b:2>
    } else { // s=0
      1:1>
    }
  }
}

func sum:3:2 {
  def{ arg:3 c:1 s:1 }

  >arg:3 0:2>
  loop 3 {
    arg:3> 0:1> >arg:3 >add1:3:2>
  }
}

func sum:4:2 #out 2:d {
  def{ arg1:2 arg2:2 x:1 ret:2 }

  >arg2:2 >arg1:2 .
  0:1> // бит переноса

  loop 2 {
    0:1> arg1:2> >x:1 >arg1:2 x:1>
    0:1> arg2:2> >x:1 >arg2:2 x:1>
    >sum:3:2>
    ret:2> >_:1 >ret:2
  }

  >_:1 .
  ret:2>
}

func sum:16:8 #out 8:d {
  def{ arg1:8 arg2:8 x:1 ret:8 }

  >arg2:8 >arg1:8 .
  0:1> // бит переноса

  loop 8 {
    0:1> arg1:8> >x:1 >arg1:8 x:1>
    0:1> arg2:8> >x:1 >arg2:8 x:1>
    >sum:3:2>
    ret:8> >_:1 >ret:8
  }

  >_:1 .
  ret:8>
}

func sum:32:16 #out 16:d {
  def{ arg1:16 arg2:16 x:1 ret:16 }

  >arg2:16 >arg1:16 .
  0:1> // бит переноса

  loop 16 {
    0:1> arg1:16> >x:1 >arg1:16 x:1>
    0:1> arg2:16> >x:1 >arg2:16 x:1>
    >sum:3:2>
    ret:16> >_:1 >ret:16
  }

  >_:1 .
  ret:16>
}

func sum:64:32 #out 32:d {
  def{ arg1:32 arg2:32 x:1 ret:32 }

  >arg2:32 >arg1:32 .
  0:1> // бит переноса

  loop 32 {
    0:1> arg1:32> >x:1 >arg1:32 x:1>
    0:1> arg2:32> >x:1 >arg2:32 x:1>
    >sum:3:2>
    ret:32> >_:1 >ret:32
  }

  >_:1 .
  ret:32>
}
