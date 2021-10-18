package it.pagopa.pdnd.interop.uservice.partymanagement.service

import akka.http.scaladsl.server.directives.FileInfo

import java.io.{ByteArrayOutputStream, File}
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import scala.collection.immutable
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

trait FileManager {

  def store(tokenId: UUID, fileParts: (FileInfo, File)): Future[OnboardingFilePath]

  def get(filePath: String): Future[ByteArrayOutputStream]

  def delete(filePath: String): Future[Boolean]

}

@SuppressWarnings(
  Array(
    "org.wartremover.warts.Any",
    "org.wartremover.warts.Throw",
    "org.wartremover.warts.StringPlusAny",
    "org.wartremover.warts.AsInstanceOf"
  )
)
object FileManager {
  private def getClassFor[T: ClassTag](fqcn: String): Try[Class[_ <: T]] =
    Try[Class[_ <: T]]({
      val c = Class.forName(fqcn, false, this.getClass.getClassLoader).asInstanceOf[Class[_ <: T]]
      val t = implicitly[ClassTag[T]].runtimeClass
      if (t.isAssignableFrom(c)) c else throw new ClassCastException(t.toString + " is not assignable from " + c)
    })

  private def createInstanceFor[T: ClassTag](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    Try {
      val types       = args.map(_._1).toArray
      val values      = args.map(_._2).toArray
      val constructor = clazz.getDeclaredConstructor(types: _*)
      constructor.setAccessible(true)
      val obj = constructor.newInstance(values: _*)
      val t   = implicitly[ClassTag[T]].runtimeClass
      if (t.isInstance(obj)) obj.asInstanceOf[T]
      else throw new ClassCastException(clazz.getName + " is not a subtype of " + t)
    }.recover { case i: InvocationTargetException if i.getTargetException ne null => throw i.getTargetException }

  private def createInstanceFor[T: ClassTag](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    getClassFor(fqcn).flatMap { c: Class[_ <: T] =>
      createInstanceFor(c, args)
    }

  def getConcreteImplementation(fileManager: String): Try[FileManager] = {
    val className = s"${this.getClass.getPackageName}.impl.${fileManager}ManagerImpl"
    createInstanceFor[FileManager](className, Nil)
  }
}
