class C(var v: Int) {
    fun foo() {
        print(<caret>v)
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://C"><code style='font-size:96%;'>C</code></a><br>public final var <b>v</b>: Int</pre></div>
